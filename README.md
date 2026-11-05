# Applied Manaita

[日本語](#日本語) | [English](#english)

まな板+ (Manaita Plus) と AE2 Unofficial Extended Life を連動させる、Minecraft 1.12.2 用のアドオンです。
An addon for Minecraft 1.12.2 that connects Manaita Plus with AE2 Unofficial Extended Life.

---

## 日本語

### 概要
まな板+ の「倍化」を AE2 の自動クラフトで使えるようにします。
まな板パターンで登録したレシピは、完成品が まな板+ の倍率倍になり、AE2 の CPU もその数で計算します。

### 必要なもの
- Minecraft 1.12.2 + Forge(または Cleanroom)
- AE2 Unofficial Extended Life
- まな板+ (Manaita Plus)
- MixinBooter(Cleanroom の場合は不要)
- JEI / HEI(任意。レシピ転送に使えます)

### 追加されるもの
| 名前 | 説明 |
|---|---|
| まな板パターンターミナル | AE2 のパターンターミナルと同じ使い方。エンコードすると まな板パターン ができます(電力・チャンネルが必要) |
| まな板パターン | クラフト用・かまど用・醸造用の3種類(AE2 のパターンと同じく中身で分かれます)。出力は まな板+ の倍率倍 |
| まな板分子組立機 | クラフト用のまな板パターンを処理します。MEインターフェースの隣に置いて使うほか、単体でも使えます。加速カード5枚まで |
| まな板分子精錬機 | かまど用のまな板パターンを、燃料なしで一瞬で処理します(MEインターフェースの隣に置く) |
| まな板分子醸造機 | 醸造用のまな板パターンを、燃料なしで一瞬で処理します(MEインターフェースの隣に置く) |

### 使い方
1. まな板パターンターミナルでレシピをエンコードします。
   - クラフトモード → クラフト用のまな板パターン
   - 加工モードで、かまどレシピ → かまど用 / 醸造レシピ → 醸造用のまな板パターン(それ以外は普通の AE2 のパターン)
2. まな板パターンを MEインターフェースに入れ、隣に対応する まな板の機械を置きます。
3. あとは AE2 の自動クラフトで注文するだけです。

- まな板パターンは普通の分子組立機では使えません。また、MEインターフェースはまな板パターンの素材を まな板の機械以外へは送りません。
- まな板パターンを作業台に1個だけ置くと、元の AE2 のパターンに戻せます。

### 倍率の設定
倍率は まな板+ の設定ファイル `config/manaita_plus.cfg` の `Doubling` から読みます。

| 設定 | 使うもの |
|---|---|
| `craftingDoubling` | クラフト用(まな板分子組立機) |
| `furnaceDoubling` | かまど用(まな板分子精錬機) |
| `brewingDoubling` | 醸造用(まな板分子醸造機) |

設定を変えると、MEインターフェースに入れたままのパターンにもすぐ反映されます。

---

## English

### Overview
Brings Manaita Plus "doubling" to AE2 autocrafting.
Recipes encoded as Manaita Patterns produce output multiplied by the Manaita Plus multiplier, and AE2 crafting CPUs plan with that amount.

### Requirements
- Minecraft 1.12.2 + Forge (or Cleanroom)
- AE2 Unofficial Extended Life
- Manaita Plus
- MixinBooter (not needed on Cleanroom)
- JEI / HEI (optional, for recipe transfer)

### Contents
| Name | Description |
|---|---|
| Manaita Pattern Terminal | Works like the AE2 Pattern Terminal, but encodes Manaita Patterns (needs power and a channel) |
| Manaita Pattern | Crafting, smelting or brewing, decided by its contents (like AE2 patterns). Output is multiplied |
| Manaita Molecular Assembler | Processes crafting Manaita Patterns. Place next to an ME Interface, or use it standalone. Up to 5 Acceleration Cards |
| Manaita Molecular Smelter | Processes smelting Manaita Patterns instantly without fuel (place next to an ME Interface) |
| Manaita Molecular Brewer | Processes brewing Manaita Patterns instantly without fuel (place next to an ME Interface) |

### How to use
1. Encode a recipe with the Manaita Pattern Terminal.
   - Crafting mode → crafting Manaita Pattern
   - Processing mode: a furnace recipe → smelting / a brewing recipe → brewing Manaita Pattern (anything else becomes a normal AE2 pattern)
2. Put the pattern into an ME Interface and place the matching Manaita machine next to it.
3. Request the item through AE2 autocrafting.

- Manaita Patterns cannot be used by the normal Molecular Assembler, and ME Interfaces never send their ingredients to anything other than Manaita machines.
- Place a single Manaita Pattern in a crafting grid to turn it back into a normal AE2 pattern.

### Multipliers
Read from the `Doubling` category of `config/manaita_plus.cfg` (Manaita Plus's config):
`craftingDoubling` (assembler), `furnaceDoubling` (smelter), `brewingDoubling` (brewer).
Changes apply immediately, even to patterns already in ME Interfaces.

---

## クレジット / Credits
- [Manaita Plus](https://www.curseforge.com/minecraft/mc-mods/manaita-plus) — リクト氏 / by Rikuto
  このmodの土台になっている「倍化」と、その設定 / The doubling feature and config this addon is built on
- [AE2 Unofficial Extended Life](https://github.com/AE2-UEL/Applied-Energistics-2) — The AE2:UEL team, AlgorithmX2 & contributors
  まな板の機械とまな板パターンターミナルの見た目には、AE2 のモデル・テクスチャを実行時に読み込んで使っています
  / Some AE2 models and textures are loaded from AE2 at runtime
- [CleanroomMC TemplateDevEnv](https://github.com/CleanroomMC/TemplateDevEnv) — 開発環境のテンプレート / Development template (MIT)
- [MixinBooter](https://github.com/CleanroomMC/MixinBooter) — Mixin の読み込み / Mixin loading

### AI の使用について / AI usage
このmodは、AIアシスタント(Anthropic の Claude)を使って開発しました。
This mod was developed with the help of an AI assistant (Anthropic's Claude).

## License
Applied Manaita is licensed under the [GNU Lesser General Public License v3.0](LICENSE).

- The build scripts are based on [CleanroomMC TemplateDevEnv](https://github.com/CleanroomMC/TemplateDevEnv) (MIT License, see [LICENSE-TemplateDevEnv](LICENSE-TemplateDevEnv)).
- This mod does not include any code or assets from Manaita Plus or AE2. Some AE2 models and textures are loaded from AE2 at runtime.
