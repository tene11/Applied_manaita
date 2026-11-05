gradle.properties の manaita_curse_file を空にしているときだけ、ここに置いた jar を開発環境 (runClient / runServer) で読み込みます。
ManaitaPlusGeneral (NeoForge 1.21.1 版) の jar を置いてください。jar は Git には入りません(.gitignore で除外)。
manaita_curse_file にファイルIDを書いたら、ここの jar は消してください(同じmodが2つになって起動できなくなります)。

Only used when manaita_curse_file in gradle.properties is empty: put the ManaitaPlusGeneral jar (NeoForge 1.21.1) here.
