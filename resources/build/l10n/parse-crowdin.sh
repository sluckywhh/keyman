#!/bin/bash
# With the Keyman translations from https://crowdin.com/project/keyman
# saved locally in /.crowdin-tmp/Keyman.zip, extract
# into /crowdin/ and move files to corresponding projects.

## START STANDARD BUILD SCRIPT INCLUDE
# adjust relative paths as necessary
THIS_SCRIPT="$(greadlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink -f "${BASH_SOURCE[0]}")"
. "$(dirname "$THIS_SCRIPT")/../build-utils.sh"
## END STANDARD BUILD SCRIPT INCLUDE

#
# Prevents 'clear' on exit of mingw64 bash shell
#
SHLVL=0

display_usage() {
  echo "parse-crowdin [-all] [-android] [-common] [-developer] [-ios]"
  echo "                     [-linux] [-mac] [-web] [-windows]"
  echo
  echo "Extract Keyman.zip at root of the repo and then copy files to given platforms"
  echo "Options:"
  echo "  -all            Copy localization files for all platforms"
  echo "  -android        Copy localization files for Android"
  echo "  -common         Copy localization files for common"
  echo "  -developer      Copy localization files for developer"
  echo "  -ios            Copy localization files for ios"
  echo "  -linux          Copy localization files for linux"
  echo "  -mac            Copy localization files for mac"
  echo "  -web            Copy localization files for web"
  echo "  -windows        Copy localization files for windows"
  exit 1
}

# Include scripts to process platforms
. ./parse-crowdin-android.sh


# Copy a file to a destination directory. If destination doesn't exist, create it
# copy_file [source file] [destination directory]
function copy_file() {
  if [ ! -d "$2" ]; then
    echo "Creating $2 directory"
    mkdir -p "$2"
  fi
  echo "Copying $1"
  cp "$1" "$2"
}



# Path definitions
KEYMAN_ZIP="$KEYMAN_ROOT/.crowdin-tmp/Keyman.zip"
KMA_ROOT="$KEYMAN_ROOT/android"
CROWDIN_ROOT="$KEYMAN_ROOT/crowdin"

if [[ $# -eq 0 ]] ; then
  display_usage
fi

# Default is to not copy to any platform
DO_ANDROID=false
DO_COMMON=false
DO_DEVELOPER=false
DO_IOS=false
DO_LINUX=false
DO_MAC=false
DO_WEB=false
DO_WINDOWS=false

# Parse args
while [[ $# -gt 0 ]] ; do
    key="$1"
    case $key in
        -all)
            DO_ANDROID=true
            DO_COMMON=true
            DO_DEVELOPER=true
            DO_IOS=true
            DO_LINUX=true
            DO_MAC=true
            DO_WEB=true
            DO_WINDOWS=true
            ;;
        -android)
            DO_ANDROID=true
            ;;
        -common)
            DO_COMMON=true
            ;;
        -developer)
            DO_DEVELOPER=true
            ;;
        -ios)
            DO_IOS=true
            ;;
        -h|-?)
            display_usage
            ;;
        -linux)
            DO_LIUNX=true
            ;;
        -mac)
            DO_MAC=true
            ;;
        -web)
            DO_WEB=true
            ;;
        -windows)
            DO_WINDOWS=true
            ;;
    esac
    shift # past argument
done

echo
echo "DO_ANDROID:   $DO_ANDROID"
echo "DO_COMMON:    $DO_COMMON"
echo "DO_DEVELOPER: $DO_DEVELOPER"
echo "DO_IOS:       $DO_IOS"
echo "DO_LINUX:     $DO_LINUX"
echo "DO_MAC:       $DO_MAC"
echo "DO_WEB:       $DO_WEB"
echo "DO_WINDOWS:   $DO_WINDOWS"
echo

# Clean /crowdin/
if [ -d "$CROWDIN_ROOT" ]; then
  echo "Cleaning $CROWDIN_ROOT"
  rm -rf "$CROWDIN_ROOT"
fi

# Extract Keyman.zip. This is the entire translation file from crowdin
echo "Unzipping Keyman.zip to $CROWDIN_ROOT"
unzip "$KEYMAN_ZIP" -d "$CROWDIN_ROOT"

if [ $? -ne 0 ]; then
    die "ERROR: Unzip Keyman.zip failed"
fi

if [ "$DO_ANDROID" = true ]; then
  processAndroid
fi


