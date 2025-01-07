# Chaging your Star-CCM Library Library
Inside the settings.json file there is a line (the second string inside "java.project.referencedLibraries") that points to MY native STAR-CCM library. If you want to edit the scripts provided in this repository, it is highly recommended you change this library path to YOUR local STAR-CCM library, as if you don't any IDE will say that all the methods used in STARs api do not exist.

## Finding the Library Directory
### Linux
Most linux users should be fine, as the STAR-CCM installation defaults to the path I have included, however if it doesnt work you can try the following commands in your terminal:
```
find {"/home","/opt"} -type d -path "*star/lib/java/platform/modules/ext" 2>/dev/null
```
this will search your root `home` and `opt` folders for the library, but doesn't guarantee its found (won't be found if its in a custom folder). If that doesnt work you can try
```
find / -type d -path "*star/lib/java/platform/modules/ext" 2>/dev/null
```
which will take singificantly longer to complete but will guarantee you find the library.

![image](Linux%20Path%20Find.png)
(Timeshift is a program for backing up my pc so ignore those results)

### Windows
For most windows users the library should be in the directory "C:/__USER__/Program Files/Siemens/__BUILD__/STAR-CCM+__BUILD__/star/lib/java/platform/modules/ext/" (where __USER__ and __BUILD__ are replaced with your pc user and star ccm build respectively). If that does not work you will need to find the directory manually.

## Adding the Library to settings.json
Once you have the library path, simply replace the path currently in settings.json with "$PATH/*" (where $PATH = library path)