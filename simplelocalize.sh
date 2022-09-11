simplelocalize download --apiKey E547A2F9F246106FffcD6869D6D84ad97d3AdFEe0A82D999961d07da2e75E3e4 \
  --downloadFormat android-xml \
  --downloadPath ./app/src/main/res/values-{lang}/strings.xml \
  --downloadOptions ESCAPE_NEW_LINES


perl -i -pe "s/'/\\\'/g" "./app/src/main/res/values-en/strings.xml"
perl -i -pe "s/'/\\\'/g" "./app/src/main/res/values-da/strings.xml"
perl -i -pe "s/'/\\\'/g" "./app/src/main/res/values-cs/strings.xml"

mv ./app/src/main/res/values-en/strings.xml ./app/src/main/res/values/strings.xml
