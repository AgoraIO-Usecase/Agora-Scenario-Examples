#!/usr/bin/python
# -*- coding: UTF-8 -*-
import re
import os

def main():
    appId = ""
    if "AGORA_APP_ID" in os.environ:
        appId = os.environ["AGORA_APP_ID"]
    token = ""

    f = open("./libs/base-library/src/main/res/values/string_configs.xml", 'r+')
    content = f.read()
    contentNew = re.sub(r'<=YOUR RTC APP ID=>', appId, content)
    contentNew = re.sub(r'<=YOUR SYNC APP ID=>', appId, contentNew)
    contentNew = re.sub(r'YOUR ACCESS TOKEN', token, contentNew)
    f.seek(0)
    f.write(contentNew)
    f.truncate()


if __name__ == "__main__":
    main()
