# Changelog

## 1.0.0 (1.20.1 Forge)
- 1.21.1 NeoForge 版をもとに、1.20.1 Forge 版を作成
  - 前提mod: AE2 15 (1.20.1 Forge 版) と、まな板mod(ManaitaPlusGeneral / ManaitaPlus Legacy / ManaitaMTK のどれか)
  - 機能は 1.21.1 版と同じ(まな板パターンターミナル・まな板クラフトパターン・まな板加工パターン・まな板分子組立機・まな板分子精錬機・まな板分子醸造機)
  - まな板mod は ManaitaPlusGeneral → ManaitaPlus Legacy → ManaitaMTK の順で、先にあるものの倍率を使う
  - ManaitaMTK にも対応(倍率は 64 倍固定)。MTK の作業台・かまどをレシピに使える
  - 醸造まな板がない環境(ManaitaMTK だけのとき)では、まな板分子醸造機のレシピを読み込まない
  - レシピ表示mod (JEI / EMI) からの転送は AE2 1.20.1 本体の連携を使う(このmod独自の JEI 連携はなし)
  - 1.20.1 の ManaitaPlus Legacy は Oculus がなくても動くので、Iris/Oculus の警告は出さない
  - パターンの中身は AE2 1.20.1 と同じく NBT に入れる
