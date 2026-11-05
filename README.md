# Applied Manaita (1.21.1 NeoForge)

[![License: LGPL v3](https://img.shields.io/badge/License-LGPL_v3-blue.svg)](COPYING.LESSER)

[日本語](#日本語) | [English](#english)

ManaitaPlusGeneral と AE2 (Applied Energistics 2) を連動させる、Minecraft 1.21.1 (NeoForge) 用のアドオンです。
An addon for Minecraft 1.21.1 (NeoForge) that connects ManaitaPlusGeneral with Applied Energistics 2.

---

## 日本語

### 概要
まな板mod の「倍化」を AE2 の自動クラフトで使えるようにします。
まな板パターンで登録したレシピは、完成品が まな板mod の倍率倍になり、AE2 の CPU もその数で計算します。

### 必要なもの
- Minecraft 1.21.1 + NeoForge
- AE2 (Applied Energistics 2) 19.2 以降
- ManaitaPlusGeneral (NeoForge 1.21.1 版)
- EMI / JEI(任意。EMI ならまな板パターンターミナルへのレシピ転送に使えます)

### 追加されるもの
| 名前 | 説明 |
|---|---|
| まな板パターンターミナル | AE2 のパターンエンコーディングターミナルと同じ使い方。エンコードすると まな板パターン ができます(電力・チャンネルが必要) |
| まな板クラフトパターン | 作業台レシピ。出力は作業まな板の倍率倍 |
| まな板加工パターン | かまどレシピか醸造レシピ。出力は調理まな板／醸造まな板の倍率倍 |
| まな板分子組立機 | まな板クラフトパターンを処理します。パターンプロバイダーの隣に置いて使うほか、単体でも使えます。加速カード5枚まで |
| まな板分子精錬機 | まな板加工パターン(かまどレシピ)を、燃料なしで一瞬で処理します(パターンプロバイダーの隣に置く) |
| まな板分子醸造機 | まな板加工パターン(醸造レシピ)を、燃料なしで一瞬で処理します(パターンプロバイダーの隣に置く) |

### 使い方
1. まな板パターンターミナルでレシピをエンコードします。
   - クラフトモード → まな板クラフトパターン
   - 加工モードで、かまどレシピか醸造レシピ → まな板加工パターン(それ以外・鍛冶台・石切台は普通の AE2 のパターン)
2. まな板パターンを パターンプロバイダー に入れ、隣に対応する まな板の機械を置きます。
3. あとは AE2 の自動クラフトで注文するだけです。

- まな板パターンは普通の分子組立機では使えません。また、パターンプロバイダーはまな板パターンの素材を まな板の機械以外へは送りません。
- まな板パターンを作業台に1個だけ置くと、元の AE2 のパターンに戻せます。

### 倍率の設定
倍率は ManaitaPlusGeneral の設定ファイル `config/manaita_plus_general-common.toml` から読みます。

| 設定 | 使うもの |
|---|---|
| `crafting_doubling_value` | まな板クラフトパターン(まな板分子組立機) |
| `furnace_doubling_value` | まな板加工パターンのかまどレシピ(まな板分子精錬機) |
| `brewing_doubling_value` | まな板加工パターンの醸造レシピ(まな板分子醸造機) |

設定を変えると、パターンプロバイダーに入れたままのパターンにもすぐ反映されます。

### 開発環境
1. `libs` フォルダに ManaitaPlusGeneral (NeoForge 1.21.1 版) の jar を置きます。
2. `gradlew runClient` でゲームを起動できます。`gradlew build` で `build/libs` に jar ができます。
3. 開発環境で動かすレシピ表示mod は `gradle.properties` の `runtime_itemlist_mod` で選べます(emi / jei / both / none)。

---

## English

### Overview
Brings ManaitaPlusGeneral "doubling" to AE2 autocrafting.
Recipes encoded as Manaita Patterns produce output multiplied by the ManaitaPlusGeneral multiplier, and AE2 crafting CPUs plan with that amount.

### Requirements
- Minecraft 1.21.1 + NeoForge
- AE2 (Applied Energistics 2) 19.2 or later
- ManaitaPlusGeneral (NeoForge 1.21.1)
- EMI / JEI (optional; EMI can transfer recipes to the Manaita Pattern Terminal)

### Contents
| Name | Description |
|---|---|
| Manaita Pattern Terminal | Works like the AE2 Pattern Encoding Terminal, but encodes Manaita Patterns (needs power and a channel) |
| Manaita Crafting Pattern | Crafting table recipes. Output is multiplied by the crafting multiplier |
| Manaita Processing Pattern | Furnace or brewing recipes. Output is multiplied by the furnace / brewing multiplier |
| Manaita Molecular Assembler | Processes Manaita Crafting Patterns. Place next to a Pattern Provider, or use it standalone. Up to 5 Acceleration Cards |
| Manaita Molecular Smelter | Processes furnace Manaita Processing Patterns instantly without fuel (place next to a Pattern Provider) |
| Manaita Molecular Brewer | Processes brewing Manaita Processing Patterns instantly without fuel (place next to a Pattern Provider) |

### How to use
1. Encode a recipe with the Manaita Pattern Terminal.
   - Crafting mode → Manaita Crafting Pattern
   - Processing mode: a furnace or brewing recipe → Manaita Processing Pattern (anything else becomes a normal AE2 pattern)
2. Put the pattern into a Pattern Provider and place the matching Manaita machine next to it.
3. Request the item through AE2 autocrafting.

- Manaita Patterns cannot be used by the normal Molecular Assembler, and Pattern Providers never send their ingredients to anything other than Manaita machines.
- Place a single Manaita Pattern in a crafting grid to turn it back into a normal AE2 pattern.

### Multipliers
Read from ManaitaPlusGeneral's config `config/manaita_plus_general-common.toml`:
`crafting_doubling_value` (assembler), `furnace_doubling_value` (smelter), `brewing_doubling_value` (brewer).
Changes apply immediately, even to patterns already in Pattern Providers.

### Development
1. Put the ManaitaPlusGeneral jar (NeoForge 1.21.1) into the `libs` folder.
2. Run `gradlew runClient` to start the game, or `gradlew build` to build the jar into `build/libs`.
3. Choose the recipe viewer for the dev environment with `runtime_itemlist_mod` in `gradle.properties` (emi / jei / both / none).

---

## クレジット / Credits
- [ManaitaPlusGeneral](https://www.curseforge.com/minecraft/mc-mods/manaita-plus-general) — RealSenPhent, gengyoubo (original Manaita Plus by Rikuto)
  このmodの土台になっている「倍化」と、その設定 / The doubling feature and config this addon is built on
- [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) — TeamAppliedEnergistics & contributors
  まな板の機械とまな板パターンターミナルの見た目・画面には、AE2 のモデル・テクスチャ・画面の配置を実行時に読み込んで使っています
  / Some AE2 models, textures and screen layouts are loaded from AE2 at runtime
- [NeoForge MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle) — 開発環境のテンプレート / Development template (MIT, see [licenses](licenses/NeoForge-MDK-TEMPLATE_LICENSE.txt))

### AI の使用について / AI usage
このmodは、AIアシスタント(Anthropic の Claude)を使って開発しました。
This mod was developed with the help of an AI assistant (Anthropic's Claude).

## License
Applied Manaita は [GNU Lesser General Public License v3.0](COPYING.LESSER) で公開しています。LGPL は GPL に条件を足したライセンスなので、[GPL-3.0](COPYING) の本文も同梱しています。
Applied Manaita is licensed under the [GNU Lesser General Public License v3.0](COPYING.LESSER), which supplements the [GNU General Public License v3.0](COPYING).

- このmodには ManaitaPlusGeneral や AE2 のアセットは入っていません(AE2 のモデル・テクスチャ・画面の配置は実行時に AE2 から読み込みます)。まな板分子組立機の処理は、AE2 の分子組立機(LGPL-3.0)を参考にしています。
- This mod does not bundle assets from ManaitaPlusGeneral or AE2 (some AE2 models, textures and screen layouts are loaded from AE2 at runtime). The Manaita Molecular Assembler logic is based on AE2's Molecular Assembler (LGPL-3.0).
