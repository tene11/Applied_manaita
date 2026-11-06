ここに置いた jar も開発環境 (runClient / runServer) で読み込みます。jar は Git には入りません(.gitignore で除外)。
まな板mod は gradle.properties の runtime_manaita_mod で自動で入るので、ここに置かないでください(同じmodが2つになって起動できなくなります)。

Jars placed here are also loaded in the dev environment. Manaita mods are added automatically via runtime_manaita_mod in gradle.properties.
