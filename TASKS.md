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
- [x] 背景の表示/透過切り替え機能（`isBackgroundVisible`/`toggleBackground`。実際のテクスチャ切り替えはCh.4）
- [x] アイテム設置ロジック: インベントリからの設置（GUIの`quickMoveStack`、中クリック消去含む）
- [ ] JEIからのドラッグ設置（Ch.5のJEI連携で対応）
- [x] GUI（メニュー+スクリーン）: 右クリックで開く、設定スロットへアイテムを入れる、中クリックで消去（`MultiItemFrameMenu`/`MultiItemFrameScreen`。背景は暫定的にバニラの`generic_54`テクスチャを流用、専用アセットはCh.4）
- [x] ハイライトカラー設定機能: モードボタンのトグル、色トグルボタン（バニラの`clickMenuButton`機構を利用、追加のネットワーキング実装は不要だった）
- [ ] インベントリ/JEIからの染料ドラッグでの色設定（現状は色トグルボタンのみ対応。ドラッグ操作はCh.5のJEI連携と合わせて検討）
- [x] 設定コピー用の共通インターフェース（`copySettings()`/`pasteSettings(CompoundTag)`をforge/neoforge双方の`MultiItemFrameEntity`に同一シグネチャで実装。実際のMemory Card/Configuration Card連携はCh.5）
- [x] ネットワーキング（GUIオープンは`ServerPlayer#openMenu`(NeoForge)/`NetworkHooks.openScreen`(Forge)のextra-data機構でエンティティIDを同期。ボタン操作はバニラの`clickMenuButton`/`handleInventoryButtonClick`で完結し、独自パケットは不要だった）
- [x] グロー版（`glow_frame_*`）の実装（`GlowMultiItemFrameEntity`、専用サウンドのみ上書き。発光の視覚表現はCh.4のレンダラー/テクスチャで対応）

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

- [ ] ブロックステート・ブロックモデル（サイズ×通常/グロー分）
- [ ] アイテムモデル（インベントリ表示用）
- [ ] テクスチャ: フレーム本体、背景表示/透過の差分、ハイライト状態（無し/フレーム/塗りつぶし）の差分、グロー発光テクスチャ
- [ ] 言語ファイル（`en_us.json`、必要なら`ja_jp.json`）
- [ ] レシピJSON・（必要なら）ルートテーブル・タグ定義

## 5. 任意依存MODとの連携

- [ ] **Applied Energistics 2**: `ae2:memory_card`によるフレーム設定のコピー＆ペースト
- [ ] **Mekanism**: `mekanism:configuration_card`によるフレーム設定のコピー＆ペースト
- [ ] **JEI**: GUI内でJEIからアイテムを選択・ドラッグして設定可能にする統合

各連携は「導入されている場合のみ有効化」される設計（`compileOnly`+実行時判定、またはローダー別の統合モジュール分離）とする。

## 6. ドキュメント・リリース関連

- [ ] `CHANGELOG.md`を新規作成（`release.yml`がバージョンごとのセクションを読み取る前提のフォーマットに合わせる）
- [ ] `docs/user-test-checklist-template.md`を新規作成（`copilot-instructions.md`のRelease Flowが参照するテンプレート）
- [ ] LICENSEファイルの要否を確認・追加

## 7. テスト環境

- [ ] `D:\curseforge\minecraft\Instances\MultiIF-Forge 1.20.1` インスタンスの存在確認・作成
- [ ] `D:\curseforge\minecraft\Instances\MultiIF-NeoForge 1.21.1` インスタンスの存在確認・作成
- [ ] 上記インスタンスへの依存MOD（AE2, Mekanism, JEI, Jade）導入・動作確認環境の準備

## 8. CI/CD

- [x] `build.yml` / `release.yml`のアーティファクト名・パスをMOD名に合わせて修正（0章参照、対応済み）
- [ ] `gradlew build --console=plain`がローカルで通ることを確認
- [ ] CurseForge / Modrinthのプロジェクト作成（`CURSEFORGE_PROJECT_ID` / `MODRINTH_PROJECT_ID`等のリポジトリ変数・シークレット設定）
