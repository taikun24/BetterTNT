# Better TNT 2.0

大量の TNT による TPS 低下を抑える Forge Mod。Minecraft 1.16.5 / 1.20.1 を Stonecutter で 1 つのソースから出す。

## 仕組み

### 1. 爆発の分散 (throttle)
1 tick あたりの爆発数と爆発処理時間に上限を設け、超えた分は次の tick 以降に回す。

- フック先は `ServerLevel.explode` (1.16.5 は `ServerWorld.explode`)。サーバー側の爆発は全てここを通る。
- 上限を超えたら `Explosion` を作らず、**呼び出しの引数を丸ごと保存**して後の tick で同じ呼び出しをやり直す。
  後回しになった爆発もバニラと同じ経路で動く (Forge の `ExplosionEvent`、ブロック破壊、ドロップ、クライアントへのパケット)。
- 順番は FIFO。キューが空でない間は、新しい爆発もその後ろに並ぶ。
- キューは各 server tick の開始時に予算いっぱいまで処理する。予算の判定は処理後なので、1 tick に最低 1 個は必ず進む。
- サーバー停止時は既定で残りを全部実行する (`flushOnShutdown`)。

### 2. 露出度キャッシュ (exposure cache)
`Explosion.getSeenPercent` (爆心からエンティティがどれだけ見えるか、を数十本のレイで調べる処理) の結果を
(ワールド, 爆心座標, エンティティの当たり判定) をキーにしてキャッシュする。同じ場所に積まれた TNT は判定がまったく同じになるので、ほとんど計算せずに済む。

ブロックが壊れると結果が変わるため、ブロックを壊した爆発の後と、各ワールド tick の境目でキャッシュを捨てる。

## 旧版 (1.0.0) からの修正点

| 旧版の問題 | 原因 | 2.0 |
|---|---|---|
| 後回しにした爆発でブロックが壊れない | `explode()` だけ呼び直していて、ブロックを壊す `finalizeExplosion()` を呼んでいなかった | 呼び出し全体をやり直す |
| 後回しにした爆発の音とパーティクルが即座に出る | `Start` イベントの時点ではもうパケット送信が確定していた | 上限超過時は `Explosion` 自体を作らない |
| 設定ファイルが効かない | `BUILDER.build()` を `define()` より前に呼んでいて spec が空。上限値も 100 で固定 | 設定項目を定義してから spec を作る |
| 専用サーバーで起動しない | クライアント専用クラス (`DebugOverlayGui`) への Mixin を共通の Mixin 一覧に入れていた | F3 表示は Forge のイベントで、クライアントでのみ登録 |
| 爆発のカウントがずれる | `ExplosionEvent` (Start と Detonate の両方) で数えていた。tick の START と END の両方で減算していた | 実際に走った爆発だけを数える |
| 後から起きた爆発が先に処理される | `push`/`pop` で LIFO になっていた | FIFO |
| 停止時にキューの爆発が消える | — | 停止時に全部実行 (設定で破棄にもできる) |

## 測定 (1.20.1 専用サーバー、25×11×25 = 6875 個の TNT ブロックに点火)

| | `/forge tps` の平均 tick (最大値) | 最低 TPS |
|---|---|---|
| バニラ (Mod の機能を全部オフ) | 180 ms | 5.5 |
| Better TNT 既定設定 | 39 ms | 20.0 |

同じ座標に TNT エンティティを 2000 個積んだ場合、露出度キャッシュのヒット率は 99.9%。サーバーの遅れは 12.3 秒から 5.6 秒になった。

その代わり、爆発が全部終わるまでの時間は長くなる (上の例ではバニラ約 25 秒、Better TNT 約 35 秒)。

## 設定
`<world>/serverconfig/bettertnt-server-config.toml`

| キー | 既定値 | 内容 |
|---|---|---|
| `throttle.enabled` | true | 分散処理を使うか |
| `throttle.maxExplosionsPerTick` | 100 | 1 tick の最大爆発数 (0 で無制限) |
| `throttle.maxExplosionMillisPerTick` | 20.0 | 1 tick に爆発へ使う最大時間 ms (0 で無制限) |
| `throttle.flushOnShutdown` | true | 停止時にキューを全部実行する (false なら破棄) |
| `exposureCache.enabled` | true | 露出度キャッシュを使うか |

## コマンド (OP 権限 2)
- `/bettertnt` — キューの数、直前の tick の爆発数と処理時間、キャッシュのヒット率を表示
- `/bettertnt flush` — キューを今すぐ全部実行
- `/bettertnt clear` — キューを破棄

シングルプレイでは F3 画面にもキューの数が出る。

## 注意
- TNT キャノンなど、爆発のタイミングが同じ tick であることを前提にした装置は、上限を超えたときに正しく動かない (旧版と同じ)。
  上限に届かなければ挙動はバニラと同じ。
- 後回しにした爆発の発生源エンティティ (点火済み TNT など) はもう消えている。ダメージの帰属 (誰が点火したか) は保たれる。

## ビルド
```
./gradlew "1.20.1:build" "1.16.5:build"
```
成果物は `versions/<mc>/build/libs/`。`./gradlew buildAndCollect` を使うと `build/libs/<version>/` にまとめて置かれる。

開発用の起動: `./gradlew "1.20.1:runClient"` / `"1.16.5:runServer"` など。実行ディレクトリは `versions/<mc>/run` で、バージョンごとに分かれている。

### ツールチェーンのメモ
- Stonecutter は **0.7.11** に固定している。0.8 以降は Gradle 9 が必要で、ForgeGradle 6 は Gradle 9 で動かない。
- Gradle は Java 21 で動かす (`gradle/gradle-daemon-jvm.properties`)。コンパイルは 1.16.5 が Java 8、1.20.1 が Java 17 (toolchain)。
- IDE で編集するバージョンを切り替えるには Stonecutter の `Set active project to <ver>` タスクを使う。コミットは 1.20.1 をアクティブにした状態で行う。
- 1.16.5 の ForgeGradle は official マッピングでも**クラス名が MCP のまま** (`ServerWorld`、`Vector3d` など)。メソッド名だけが Mojang 名になる。
