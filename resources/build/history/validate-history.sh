echo "**TODO** Update script to work with new history/versioning"
exit 1

_hu_base_dir=$(dirname "$BASH_SOURCE")/..

. $_hu_base_dir/resources/historyUtils.sh

# Insert the shorthand for a platform as the command-line argument.
validate_history_file "$1"