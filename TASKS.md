# Multi Item Frame — 開発タスク洗い出し

現状、リポジトリには `README.md` / `.editorconfig` / `.github/**`（instructions・workflows）のみが存在し、Gradleプロジェクトや実装コードは一切存在しない。以下は、README・`copilot-instructions.md`・既存ワークフローから読み取れる仕様をもとにした、ゼロからの実装に必要なタスクの洗い出し。

## 0. 前提の確認・矛盾点の解消

- [x] `README.md`の`Supported targets`表（NeoForge 1.21.1 / Forge 1.20.1のみ）と、`.github/copilot-instructions.md`の`Supported Loaders`（NeoForge 1.21.1 / **NeoForge 26.1.2** / Forge 1.20.1）の食い違いを確認し、NeoForge 26.1.2（`neoforge2612`モジュール）を今回のスコープに含めるか決定する
- [x] `README.md`のOptional Dependenciesに`Jade 🔍`が挙げられているが、対応する機能説明が本文に無い。Jade連携（例: フレームの中身をツールチップ表示するWailaプロバイダ）を実装するか、記載を削除するか方針を決める
- [x] `.github/workflows/build.yml`・`release.yml`内のアーティファクト名・パスを`multiitemframe-forge-1.20.1-*.jar` / `multiitemframe-neoforge-1.21.1-*.jar`に修正し、`neoforge2612`除外を前提とした古いコメントも削除した
- [x] `release.yml`の`mc-publish`ステップの`dependencies: mekanism(required)`を、README.mdの記載（Mekanismはoptional）に合わせて`mekanism(optional)`に修正した
- [x] `copilot-instructions.md`のコメント（Mekanismバージョン未リリースのため`neoforge2612`を除外、という注記）も別プロジェクト由来と思われる内容。Multi Item Frame用に書き直すか削除する

## 1. プロジェクト基盤（マルチローダー構成）

- [x] Gradleマルチモジュール構成を新規作成: `common` / `forge` / `neoforge`
- [x] `settings.gradle` / `build.gradle`（ルート・各モジュール）を作成し、Forge 1.20.1 / NeoForge 1.21.1向けのビルド設定を行う
- [x] `gradle.properties`を作成し、MOD ID・バージョン・対象MC/ローダーバージョン等を定義（`release.yml`が`version=`を参照する前提）
- [x] Gradle Wrapper（`gradlew` / `gradlew.bat` / `gradle/wrapper/*`）を追加
- [x] `.gitignore`を作成（`build/`, `.gradle/`, `run/`等）
- [x] MOD ID（`multiitemframe`）とMOD名・説明・作者等のメタデータを確定（group=`wtf.vd`, mod_id=`multiitemframe`, mod_author=`Vee Dee`, license=`LGPL-3.0`）
- [x] Forge用`mods.toml`、NeoForge用`neoforge.mods.toml`を作成
- [x] アイコン画像等のMODメタデータアセットを準備（仮のプレースホルダー画像を配置。正式なアイコンは別途デザインが必要）

`.\gradlew.bat build --console=plain`でビルド成功を確認済み（`multiitemframe-forge-1.20.1-1.0.0.jar` / `multiitemframe-neoforge-1.21.1-1.0.0.jar`を生成、`build.yml`/`release.yml`が期待するファイル名と一致）。`LICENSE`（LGPL-3.0）も追加済み（ビルドタスクが参照するため前倒しで作成）。

## 2. コアロジック（`common`モジュール）

> **設計変更（Ch.2着手時に判明）**: `common`は2ローダー分としてソースが2回コンパイルされるが、Forge 1.20.1とNeoForge 1.21.1はMC 1.20.5の Data Components導入により`ItemFrame`/`HangingEntity`系のバニラAPIが大きく乖離しており（`HangingEntity`の親クラス、`defineSynchedData`のシグネチャ、`ItemStack`のNBT保存/復元、`getAddEntityPacket`等）、フレーム本体のEntity/Item/GUIクラスは`common`に置けない。よって`common`には`FrameSize`/`HighlightMode`等のMC非依存な純粋データのみを置き、Entity/Item/Menu/Screen/レンダラー/登録処理はforge・neoforge双方に個別実装する方針とした（ユーザー承認済み）。

- [x] Item Frame拡張エンティティの基本設計（`HangingEntity`を直接継承、`common`に`FrameSize`/`HighlightMode` enumを配置）
- [x] サイズバリエーションの実装: `1x1` / `1x2` / `2x1` / `1and2` / `2and1` / `2x2`（`FrameSize` enum、スロット数・グリッド位置を保持）
- [x] 各サイズごとのアイテム・エンティティ登録、およびNBT保存/復元（forge/neoforgeそれぞれに`MultiItemFrameEntity`実装。エンティティ種別は`multi_item_frame`/`glow_multi_item_frame`の2つのみで、サイズは同期エンティティデータとして保持）
- [x] 専用クリエイティブタブ（`ModCreativeTabs`、forge/neoforge双方）にアイテム12種を列挙。未登録だとクリエイティブインベントリにもJEIにも一切表示されない不具合があり、テスト環境での検証で発覚したため追加。
- [x] 背景の表示/透過切り替え機能（`isBackgroundVisible`/`toggleBackground`。実際のテクスチャ切り替えはCh.4）
- [x] アイテム設置ロジック: インベントリからの設置（GUIの`quickMoveStack`、中クリック消去含む）
- [x] JEIからのドラッグ設置（実アイテムスロットへのドラッグはJEI標準機能でそのまま動作。Ch.5のJEI連携で染料ドラッグ用のゴーストターゲットのみ追加実装）
- [x] GUI（メニュー+スクリーン）: 右クリックで開く、設定スロットへアイテムを入れる、中クリックで消去（`MultiItemFrameMenu`/`MultiItemFrameScreen`。背景は暫定的にバニラの`generic_54`テクスチャを流用、専用アセットはCh.4）
- [x] ハイライトカラー設定機能: モードボタンのトグル、色トグルボタン（バニラの`clickMenuButton`機構を利用、追加のネットワーキング実装は不要だった）。`gui_sample.html`（モックアップ、`.gitignore`対象外の作業用ファイル）でのUI方針確認を受け、`HighlightMode`を`NONE/FRAME/FILL`の3状態から`FRAME/FILL`の2状態トグルに変更し、「ハイライトなし」は色を未設定（transparent）にすることで表現するよう仕様変更。GUIレイアウトも、各スロットのアイテム欄＋モードボタン＋色ボタンを横一列の「行」としてまとめ、`FrameSize`が1列/1行しかない場合はその行を2列/2行分の領域内で中央寄せする方式（`FrameSize#columnSpan()`/`rowSpan()`）に刷新し、全サイズで同じ大きさ・形のパネルになるよう統一（既存の重なり・はみ出し問題を解消）。モード/色ボタンはテキストボタンから、`common/.../assets/multiitemframe/gui/`に配置済みの16x16アイコン（ハイライトモード2種、色17種＝透明+ダイ16色、ボタン背景2種）を使う独自`IconButton`ウィジェット（forge/neoforge双方）に置き換え。
- [x] インベントリ/JEIからの染料ドラッグでの色設定（色トグルボタンに加え、JEIの`IGhostIngredientHandler`で染料をボタンへ直接ドラッグ&ドロップ可能。インベントリからのドラッグは通常のドラッグ&ドロップに対応する専用UIが無いため対象外、ボタンクリックでの巡回設定を継続採用）
- [x] 設定コピー用の共通インターフェース（`copySettings()`/`pasteSettings(CompoundTag)`をforge/neoforge双方の`MultiItemFrameEntity`に同一シグネチャで実装。実際のMemory Card/Configuration Card連携はCh.5）
- [x] ネットワーキング（GUIオープンは`ServerPlayer#openMenu`(NeoForge)/`NetworkHooks.openScreen`(Forge)のextra-data機構でエンティティIDを同期。ボタン操作はバニラの`clickMenuButton`/`handleInventoryButtonClick`で完結し、独自パケットは不要だった）
- [x] グロー版（`glow_frame_*`）の実装（`GlowMultiItemFrameEntity`、専用サウンドのみ上書き。発光の視覚表現はCh.4のレンダラー/テクスチャで対応）
- [x] **アイテム欄のゴースト化（実機テストでのバグ報告を受けて実施）**: フレームのアイテムスロットは実アイテムを保持せず、「表示するアイテムの種類」のみを記憶するゴーストスロットに変更（`MultiItemFrameMenu#clicked`/`quickMoveStack`を全面書き換え。左クリックでカーソルのアイテムを1個分だけ複製表示、カーソル側は変化なし。中クリックで表示解除、shift+クリックは無効化）。ドロップ時に実アイテムが消費される・個数情報が失われるという不具合、およびフレーム破壊時に見せかけのアイテムが実体としてドロップされる不具合を解消（`MultiItemFrameEntity#dropItem`からも表示アイテムのドロップ処理を削除）。JEI/インベントリからの直接設定は新設の`DIRECT_ITEM_BASE`ボタンID範囲（`MultiItemFrameMenu#setDisplayItemDirect`、`BuiltInRegistries.ITEM`の登録IDを`clickMenuButton`経由で送信）で対応。

`.\gradlew.bat build --console=plain`でforge/neoforge双方のコンパイル・ビルド成功を確認済み。エンティティのレンダラーは未描画のプレースホルダー（`MultiItemFrameRenderer`、テクスチャ・モデルはCh.4）。

## 3. クラフトレシピ

- [x] `Multi Item Frame 1x1`: Item Frame + Redstone Dust（シェイプレス）
- [x] `1x2`（縦2連結）/ `2x1`（横2連結）レシピ、および両者間の単一クラフトによる相互変換（1,2キー分クラフトグリッドに置くだけで変換）
- [x] `1,2`（上1下2）/ `2,1`（上2下1）レシピ（複数パターン: 1x1×3個の並び、または1x1+1x2の組み合わせ）
- [x] `2x2`レシピ（複数パターン: 1x1×4、1x2×2、2x1×2）
- [x] グロー版レシピ: `Glowing 1x1` = 非グロー1x1 + Glowstone Dust（シェイプレス）、他サイズは対応する非グロー版のグロー変換
- [x] レシピの実装後、README記載のクラフト図と実際のレシピJSONの整合性を確認

`tools/generate_recipes.py`でforge/neoforge双方のレシピJSON（各24個）を生成。**Forge 1.20.1とNeoForge 1.21.1でレシピJSONスキーマ自体もMC 1.21で変更されている**（`result`の`item`→`id`キー変更、ingredientの`{"item":...}`→プレーン文字列化）ため、Ch.2のJavaコードと同様に`common`の共有リソースではなくローダー別に生成した（スクリプト側で`legacy`フラグにより両スキーマを出し分け）。`.\gradlew.bat build`でリソース処理・ビルド成功を確認済み。実際にゲーム内でレシピが認識されるかの検証はCh.7のテスト環境で実施。

## 4. アセット

- [x] ~~ブロックステート・ブロックモデル~~ — Ch.2でエンティティとして実装したため対象外（ブロックではない）
- [x] アイテムモデル（インベントリ表示用、12種すべて `item/generated` 参照のプレースホルダーを配置。`common/src/main/resources/assets/multiitemframe/models/item/`）
- [x] テクスチャ: プレースホルダーを配置済み（`common/src/main/resources/assets/multiitemframe/textures/`）。フレーム本体アイテムアイコン12種（`item/`）、インワールド用フレーム本体・グロー発光（`entity/frame.png`・`frame_glow.png`）、背景表示用（`entity/background.png`）、ハイライト用の枠・塗りつぶし各1枚（`entity/highlight_frame.png`・`highlight_fill.png`、いずれも白色で描画時にスロットごとの`DyeColor`へ着色する想定、16色分を個別に用意しない）。
- [x] インワールド描画ロジック（`MultiItemFrameRenderer`、forge/neoforge双方）: 手動`VertexConsumer`でフラットな板ポリゴンを積層描画。`isBackgroundVisible()`ならスロットごとに`background.png`、`FrameSize`の外接矩形全体に`frame.png`/`frame_glow.png`（グロー版は`GlowMultiItemFrameEntity`判定で切替）、スロットごとに`HighlightMode`（`FRAME`/`FILL`）に応じた`highlight_frame.png`/`highlight_fill.png`を頂点カラーで`DyeColor`着色、最後に`ItemRenderer.renderStatic`でスロットのアイテムを描画。設置面の直交ベクトル（`getYRot()`から算出）に沿って板を向け、Z方向にわずかなオフセット（`WALL_OFFSET`/`LAYER_STEP`）でZファイティングを回避。NeoForge側は1.21.1で刷新された`VertexConsumer`API（`addVertex`/`setColor`/`setUv`/`setNormal`、`.endVertex()`廃止）に合わせて実装（Forge側は旧来の`.vertex(...).color(...).endVertex()`チェーン）。常時発光（光源化）は別課題として未着手（Ch.4当初の議論参照）。
- [x] ~~設置面オフセットのバグ修正（`WALL_OFFSET`による回転前ワールド空間オフセット）~~ — 下記「二重オフセットバグの根本修正」で完全に置き換え済み（この対症的な修正自体が、後述する二重オフセットの原因だったことが判明したため）。
- [x] **フレーム不可視・位置ズレの根本原因調査と修正（実機テストで正面から完全に不可視、背面から傾いた状態で見える不具合の再報告を受けて実施）**: バニラの`HangingEntity`/`EntityRenderDispatcher`/`ItemFrameRenderer`をデコンパイルして比較した結果、`HangingEntity#recalculateBoundingBox()`（継承元のバニラ実装、未override）がエンティティ自身のワールド座標を既に設置面に密着する位置へ補正済みであり、`EntityRenderDispatcher.render()`はその座標（+`getRenderOffset()`、本レンダラーは未overrideのためゼロ）でPoseStackを平行移動してから`render()`を呼び出す——つまり本来`render()`内で追加の平行移動は一切不要だった。前回セッションの「設置面オフセットのバグ修正」がバニラの`ItemFrameRenderer`の平行移動処理だけを模倣し、それとセットの`getRenderOffset()`オーバーライド＆キャンセル処理を伴わなかったため、実質的に二重にオフセットがかかり、フレームが壁から`WALL_OFFSET`(約0.47ブロック)分浮いた位置に描画されていた（不可視・傾いて見える等の症状の直接原因）。**修正**: `render()`内の回転前の平行移動処理を完全に削除（forge/neoforge双方）。エンティティ自身の位置（ディスパッチャが渡す座標）をそのまま使い、Y軸回転のみ適用する方式に変更。
- [x] **フレーム本体の厚み表現（1px、設置ブロックへの密着）を追加**: `THICKNESS = 0.0625F`(1px)の箱形状として、正面（既存`frame.png`/`frame_glow.png`）・背面（同テクスチャを頂点順序反転で描画、`RenderType.entityCutout()`が背面カリングを行うため裏面から見た際の不可視を回避）・上下左右4枚の側面帯（新規`frame_side.png`/`frame_glow_side.png`、暫定プレースホルダー）を描画するよう`MultiItemFrameRenderer`を拡張（forge/neoforge双方）。背景・ハイライト・アイテムの各レイヤーは正面の厚み分（`HALF_THICKNESS`）を基準に積層するよう深度計算を変更。側面用テクスチャは単色プレースホルダーのため、実際のアート差し替えを推奨（板材・金属エッジ風のタイル可能なテクスチャを想定）。
- [x] **1and2/2and1の単独スロットの中央寄せ**: `FrameSize.slotPositions`の型を`int[][]`から`double[][]`に変更し、`ONE_AND_TWO`/`TWO_AND_ONE`の単独スロット座標を`0.5`（半セル分）に変更することで、ワールド描画・GUIレイアウトの両方でコードを分岐させずに中央寄せを実現（`MultiItemFrameMenu`のGUIピクセル計算、`MultiItemFrameRenderer`のワールド座標計算の双方を`double[]`対応に更新）。新規アセットは不要（純粋なレイアウト計算のみ）。
- [x] **複数スロットフレームのアイテム描画スケール変更**: 1x1フレームは既存通り`0.5`倍（バニラのアイテムフレームと同等の見た目を維持）、2スロット以上のフレームは`0.25`倍（ユーザー要望の「50%に縮小」＝既存0.5倍からさらに半分）に変更。
- [x] **設置footprint（当たり判定）が1x1ブロックを超えてしまうバグの修正**: `getWidth()`/`getHeight()`（Forge、`HangingEntity`のピクセル単位オーバーライド）・`calculateBoundingBox()`（NeoForge、1.21.1のAABB直接算出オーバーライド）が`FrameSize.columns()`/`rows()`をそのまま当たり判定サイズに使っていたため、2x2や1x2のフレームは実際に2x2/1x2ブロック分の設置クリアランス・当たり判定を要求してしまい（バニラのPaintingと同じ仕組みを誤って流用していた）、かつ`HangingEntity`の壁密着センタリング計算（幅/高さに依存）が乱れて非1x1サイズの描画位置がズレる原因にもなっていた。複数スロットのグリッド分割はあくまで1ブロック面の中の「見た目上の」区切りであり実際の設置は常に1x1ブロックであるべきと判断し、当たり判定サイズを常に固定1（バニラのアイテムフレームと同じ）に変更（forge/neoforge双方）。
- [x] **正面からアイテム・ハイライトが見えず背面からは見える不具合の修正**: `RenderType.entityCutout()`のカリング挙動・バニラ`ItemFrameRenderer`のオフセット規約をデコンパイルで裏取りした上で原因を特定 — 積層レイヤー間のZオフセット（`LAYER_STEP=0.002`）が小さすぎ、通常の視認距離ではデプスバッファの精度不足によりZファイティングが発生し、視点や描画順（グロー版/非グロー版でテクスチャが異なるだけで座標は同一のはずが、結果が食い違っていたのはこのため）によって前面パネルとアイテム/ハイライトのどちらが手前に描画されるか不安定になっていたことが原因。`LAYER_STEP`を`0.03`に拡大し、各レイヤー間に十分なZマージンを確保することで解消（forge/neoforge双方）。
- [x] ~~上記のLAYER_STEP拡大による修正~~ — 実機再テストで改善せず、根本原因は別にあった。詳細は下記2件を参照。
- [x] **フレームの見た目サイズが常に1ブロックに収まらず、`FrameSize`の列数/行数に比例して複数ブロックに跨って描画されていたバグの修正**: 当たり判定（Ch.直前で修正済み）とは独立に、レンダラー自体が`halfWidth = size.columns()/2`・`halfHeight = size.rows()/2`という「1スロット=1ブロック」の座標系で描画しており、2x2フレームは見た目上も2x2ブロック分の面積を占めていた（バニラのPaintingのような複数ブロック表示を意図せず再現してしまっていた）。ユーザー要件は「どのサイズも常に1x1ブロックの面の中に収まり、スロット分割は1ブロック内部でのグリッド区切りに過ぎない」ことだったため、外枠のボックス形状を常に固定`-0.5..0.5`（1x1ブロック）とし、各スロットは`1/columns × 1/rows`のセルサイズでその内部を分割するよう再設計。アイテムスケールも`0.5 × min(セル幅, セル高さ)`という単一の式に統一（1x1なら`0.5`でバニラ相当、2x2なら`0.25`など、セルが小さくなるほど自動的に縮小するため、以前ハードコードしていた複数スロット用`0.25`固定値は撤廃）。
- [x] **正面が見えずアイテム・ハイライトが背面からしか見えない不具合の再修正**: LAYER_STEP拡大では改善しなかったため、Zファイティングではなく頂点巻き順（ワインディング）とバックフェイスカリングの不整合が原因と判断。自前で前面・背面の両方を明示的に描画しているためGPUのバックフェイスカリングに頼る必要がなく、巻き順の解析にリスクが伴う（デコンパイルでの検証だけでは実機の挙動と食い違う結果となった）ため、フレーム本体・背景・ハイライトの描画に使う`RenderType`を`entityCutout()`（カリングあり）から`entityCutoutNoCull()`（カリングなし）に変更し、巻き順に依存せず両面とも確実に描画されるようにした（forge/neoforge双方）。
- [x] ~~上記のNoCull化による修正~~ — 実機再テストでも改善せず（アイテム・ハイライトは相変わらず「裏」＝ガラス越しに見える側にのみ表示され、実際にGUIを開く「表」側では見えないまま）。カリングの有無ではなく、背景・ハイライト・アイテムの各レイヤーを積層するZ方向（`depth`変数の符号）そのものが、本来アクセス可能な側と逆になっていたことが原因と判明。バニラの`ItemFrameRenderer`をデコンパイルして同じ回転式（`Axis.YP.rotationDegrees(180 - yaw)`）・同じ正方向（`+Z`）のアイテム配置を確認した上で理論的に検証しても矛盾は見当たらなかったため（`HangingEntity.setDirection`のyaw計算・`Direction.get2DDataValue()`の値まで含めて追った）、これ以上のワインディング/軸解析には踏み込まず、実機での見え方（「裏」に出る＝ガラス越しに見た際に閉塞されずに見えている）を直接の判断材料として、`depth`の符号を反転（`+Z`側ではなく`-Z`側へ積層）する修正に切り替えた。フレーム本体の前面・背面テクスチャ自体は対称（同じ`frame.png`を両面に描画）のため、内容物側のレイヤーだけを反転させても見た目に矛盾は生じない（forge/neoforge双方、`MultiItemFrameRenderer#render`の`depth`初期値・増減方向・アイテム用オフセットの符号をすべて反転）。ビルド・デプロイ済み、実機再検証待ち。
- [x] **GUI刷新（実機テストでのGUI重なり・操作不能の報告を受けて実施）**: `base.png`（256x256の枠+背景一体型テクスチャ）を`renderBg`でパネルサイズにストレッチ描画するよう変更（旧`generic_54.png`から差し替え）。プレイヤーインベントリとの重なりを解消するレイアウト調整（`GRID_ORIGIN_Y`を18→22に変更）。「Background」トグルボタンは用途不明のためGUIから削除（`isBackgroundVisible`データ自体は温存、GUI操作口のみ撤去）。`IconButton`にクリック中のみ`_pressing.png`を表示する状態管理、中クリックコールバック、状態依存の動的ツールチップ（`Tooltip.create`を毎フレーム更新）を追加。アイテムスロットのツールチップ「Drag and Drop to choose item, Middle-click to erase」、ハイライトモードボタンのツールチップ「Highlight type: Frame」/「Hightlight type: Filled」（ユーザー指定の原文ママ、誤字含む）を追加。アイテムスロット・色ボタンとも中クリックで初期化（未設定/透明）に対応するため、`Screen#mouseClicked`をオーバーライドしてバニラのクリエイティブ限定ゲートを回避し、中クリックを`handleInventoryMouseClick`経由で常に転送するよう変更。
- [x] **新GUIアセット（`main_gui_background.png`ほか）に合わせたレイアウト再実装**: ユーザーが新規テクスチャ一式（`main_gui_background.png`＝176x166相当のパネル本体、内部の設定エリアは透明な「ビューポート」でバニラ準拠のプレイヤーインベントリ描画がそのまま透けて見える構造、`main_gui_{1x1,1x2,2x1,2x2,1and2,2and1}_placeholder.png`＝各`FrameSize`ごとのスロット配置を示す開発用レイアウトガイド（実テクスチャではなく、四隅のドット+区切り線のみのピクセル座標参照用画像）、`button_stack.png`＝1スロット分のウィジェット群（アイテムスロット+ハイライト種別ボタン+色ボタン、各18x18・ギャップ3pxで横並び、合計60x18）のレイアウト見本、`item_slot_background.png`＝アイテムアイコン背景（16x16）、`button_background.png`/`button_background_pressing.png`＝ボタン背景を16x16→18x18に拡大）を追加。全プレースホルダー画像をPythonでピクセル解析し、各`FrameSize`のスロット配置座標を逆算（`GRID_ORIGIN_X/Y=7`, `CELL_WIDTH=80`, `CELL_HEIGHT=36`, `GROUP_WIDTH=60`, `GROUP_HEIGHT=18`）。`MultiItemFrameMenu`のグリッド計算式を、ウィジェット群自体の大きさ（`GROUP_WIDTH/HEIGHT`）とグリッド間隔（`CELL_WIDTH/HEIGHT`）を別々の定数として扱うよう修正（旧実装は両者を同一の定数として混同しており、新しい非正方形グリッドに対応できなかった）。`MultiItemFrameScreen`の背景テクスチャを`main_gui_background.png`に差し替え（ソース画像サイズ333x256）、ボタンサイズを16→18pxに拡大、アイテムスロット背景を新設の`item_slot_background.png`で明示的に描画するよう変更（新パネルの設定エリアが透明なため）。`IconButton`もボタンサイズ18x18＋中央に16x16アイコンを描画する構成に更新（forge/neoforge双方）。ビルド確認済み・実機未検証（ユーザーへの意図確認は完了、実装後の見た目レビュー待ち）。
- [x] JEI連携を実アイテムスロット向けに拡張（Ch.5の染料専用ゴーストハンドラを一般化し、任意アイテムのドラッグ&ドロップでアイテムスロットへの表示アイテム設定にも対応。詳細はCh.5参照）。
- [x] 言語ファイル（`en_us.json`）は既存のforge/neoforge双方の`assets/multiitemframe/lang/en_us.json`に集約（Ch.2時点で作成済みだったため、Ch.5で追加した`config_card_*`キーの翻訳文言を今回追記）。`ja_jp.json`は未着手（必要になれば別途）。
- [x] レシピJSON・タグ定義はCh.3で完了済み（ルートテーブルは対象外、右クリックで取得するため不要）。
- 生成用ツール: `tools/generate_placeholder_assets.py`（再実行で全プレースホルダーを再生成可能。本物のアートに差し替える際は、上記の着色前提テクスチャの仕組みを踏襲すること）。

## 5. 任意依存MODとの連携

- [x] **Applied Energistics 2**: `ae2:memory_card`によるフレーム設定のコピー＆ペースト（`Ae2MemoryCardCompat`、forge/neoforge双方）
- [x] **Mekanism**: `mekanism:configuration_card`によるフレーム設定のコピー＆ペースト（`MekanismConfigCardCompat`、forge/neoforge双方）
- [x] **JEI**: GUI内でJEIからアイテムを選択・ドラッグして設定可能にする統合（実アイテムスロットへのドラッグはJEI標準機能。染料ドラッグによる色設定は`IGhostIngredientHandler`で追加実装）

各連携は「導入されている場合のみ有効化」される設計（`compileOnly`+`ModList.get().isLoaded(...)`実行時判定、`compat`パッケージにローダー別実装）で対応済み。

AE2/Mekanism連携の実装メモ:
- AE2はForge/1.20.1（15.x系）とNeoForge/1.21.1（19.x系）で`IMemoryCard`のストレージAPIが非互換（旧: `setMemoryCardContents`による自由なCompoundTag保存、新: `IUpgradeableObject`/`IConfigurableObject`/`IPriorityHost`/`IConfigInvHost`のみを対象とするDataComponentベースの固定スキーマで、任意NBT保存の手段が廃止されている）。本MODはEntityであり、いずれのAPIにも自然には乗らないため、`IMemoryCard`は型判定と`notifyUser`によるメッセージ表示のみに使い、実データは独自の名前空間タグキー（`multiitemframe:frame_settings`）でカードのItemStackに直接保存する方式に統一した（両ローダーで完全に同一の挙動）。
- Mekanismの`IConfigCardAccess`（Configuration Card連携用capability）は`BlockEntity`のみを対象とする設計（`ItemConfigurationCard.useOn`経由のcapability lookup）で、Entityである本MODには発火しないため、Mekanism公式のディスパッチ機構は使わず、アイテムの登録名（`mekanism:configuration_card`）による直接判定と、Mekanism本体のカードNBT構造（`mek_data`/`data`/`data_name`。Forge/1.20.1は`mekanism.api.NBTConstants`、NeoForge/1.21.1は`mekanism.api.SerializationConstants`とキー名が変わっている）を模倣した独自書き込みで対応。
- NeoForge/1.21.1側はvanillaのData Components移行に伴い、ItemStackへの自由なNBT保存は`DataComponents.CUSTOM_DATA`（`CustomData.of(tag)`/`copyTag()`）経由で行う。
- チャットメッセージ（保存/読込/不正カード）は`gui.multiitemframe.config_card_saved`等の翻訳キーを使用しており、対応する翻訳文言はCh.4で両ローダーの`en_us.json`に追記済み。

JEI連携の実装メモ:
- `mezz.jei:jei-*-forge-api`/`jei-*-neoforge-api`はローダー固有の薄いシムのみを含み、`IModPlugin`/`IGhostIngredientHandler`等の本体APIは別アーティファクト`mezz.jei:jei-*-common-api`にある。両`build.gradle`に`compileOnly`を追加した。
- アイテムスロットはゴーストスロット化（Ch.2参照）に伴い実アイテムを保持しないため、JEI標準のスロットドラッグ&ドロップは対象外。代わりに`IGhostIngredientHandler<MultiItemFrameScreen>`（`compat.jei.MultiItemFrameJeiPlugin`、`@JeiPlugin`で自動検出）を汎用アイテム対応に拡張し、任意のアイテムをアイテムスロットへドラッグすると表示アイテムを直接設定（`DIRECT_ITEM_BASE`ボタン範囲）、染料の場合はさらに色トグルボタンへのドラッグにも対応（`DIRECT_COLOR_BASE`ボタン範囲）するようにした。

## 6. ドキュメント・リリース関連

- [x] `CHANGELOG.md`を新規作成（`release.yml`がバージョンごとのセクションを読み取る前提のフォーマットに合わせる）
- [x] `docs/user-test-checklist-template.md`を新規作成（`copilot-instructions.md`のRelease Flowが参照するテンプレート）
- [x] LICENSEファイルの要否を確認・追加（Ch.1で`LGPL-3.0`のLICENSEを前倒しで作成済み。追加対応不要）

## 7. テスト環境

- [x] `D:\curseforge\minecraft\Instances\MultiIF-Forge 1.20.1` インスタンスの存在確認・作成（既存を確認済み）
- [x] `D:\curseforge\minecraft\Instances\MultiIF-NeoForge 1.21.1` インスタンスの存在確認・作成（既存を確認済み）
- [x] 上記インスタンスへの依存MOD（AE2, Mekanism, JEI, Jade）導入・動作確認環境の準備（Forge側はAE2/Mekanism/JEI/Jade導入済み、NeoForge側はAE2/Mekanism/JEI導入済み。Jadeはこのプロジェクトの依存関係ではないため必須ではない）。両インスタンスの`mods`フォルダに最新ビルドの`multiitemframe-*.jar`を配置済み。実際の起動・手動テストはユーザー側で実施。

## 8. CI/CD

- [x] `build.yml` / `release.yml`のアーティファクト名・パスをMOD名に合わせて修正（0章参照、対応済み）
- [x] `gradlew build --console=plain`がローカルで通ることを確認（このセッションで複数回確認済み。直近: Ch.5完了時点でBUILD SUCCESSFUL）
- [ ] CurseForge / Modrinthのプロジェクト作成（`CURSEFORGE_PROJECT_ID` / `MODRINTH_PROJECT_ID`等のリポジトリ変数・シークレット設定）
