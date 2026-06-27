#!/sbin/sh

#################
# Initialization
#################

umask 022

# echo before loading util_functions
ui_print() { echo "$1"; }

require_new_supersu() {
  ui_print "*******************************"
  ui_print " Please install SuperSU v20.4+! "
  ui_print "*******************************"
  exit 1
}

#########################
# Load util_functions.sh
#########################

OUTFD=$2
ZIPFILE=$3

mount /data 2>/dev/null

[ -f /data/adb/supersu/util_functions.sh ] || require_new_supersu
. /data/adb/supersu/util_functions.sh
[ $SUPERSU_VER_CODE -lt 20400 ] && require_new_supersu

install_module
exit 0
