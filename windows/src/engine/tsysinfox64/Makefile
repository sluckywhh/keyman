#
# TSysInfox64 Makefile
#

!include ..\..\Defines.mak

build: version.res manifest.res dirs
    -md x64
    $(DCC64) tsysinfox64.dpr
    $(COPY) x64\tsysinfox64.exe $(PROGRAM)\engine

clean: def-clean

signcode:
    $(SIGNCODE) /d "Keyman Engine x64 Diagnostics" $(PROGRAM)\engine\tsysinfox64.exe

backup:
    $(WZZIP) $(BUILD)\engine\tsysinfox64.zip  $(BACKUPDEFAULTS) x64\*.map x64\tsysinfox64.exe

install:
    echo Nothing to do.

test-manifest:
    # test that linked manifest exists and correct
    $(MT) -nologo -inputresource:$(PROGRAM)\engine\tsysinfox64.exe -validate_manifest

!include ..\..\Target.mak
