set PRODUCTVERSION=3.2.0
"%WIX%"\bin\heat dir ctfscreen-data -ag -dr dir_ctfscreen_data -suid -cg libDir -out libDir.wxs
"%WIX%"\bin\candle -arch x64 libDir.wxs
"%WIX%"\bin\candle installer.wxs
"%WIX%"\bin\light installer.wixobj libDir.wixobj -b ctfscreen-data -out ctfscreen-%PRODUCTVERSION%.msi
del installer.wixobj libDir.wixObj ctfscreen-%PRODUCTVERSION%.wixpdb
