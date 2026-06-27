mount_tmpfs() {
  mv supersu supersu.tmp
  mount -t tmpfs -o 'mode=0755' supersu $1
  mv supersu.tmp supersu
}

mount_sbin() {
  mount_tmpfs /sbin
  chcon u:object_r:rootfs:s0 /sbin
}

if [ ! -f /system/build.prop ]; then
  echo 'Please run `./build.py emulator` instead of directly executing the script!'
  exit 1
fi

cd /data/local/tmp
chmod 755 busybox

if [ -z "$FIRST_STAGE" ]; then
  export FIRST_STAGE=1
  export ASH_STANDALONE=1
  if [ $(./busybox id -u) -ne 0 ]; then
    exec /system/xbin/su 0 /data/local/tmp/busybox sh $0
  else
    exec ./busybox sh $0
  fi
fi

pm install -r -g $(pwd)/supersu.apk

unzip -oj supersu.apk 'assets/util_functions.sh' 'assets/stub.apk'
. ./util_functions.sh

api_level_arch_detect

unzip -oj supersu.apk "lib/$ABI/*" -x "lib/$ABI/libbusybox.so"
for file in lib*.so; do
  chmod 755 $file
  mv "$file" "${file:3:${#file}-6}"
done

if $IS64BIT && [ -e "/system/bin/linker" ]; then
  unzip -oj supersu.apk "lib/$ABI32/libsupersu.so"
  mv libsupersu.so supersu32
  chmod 755 supersu32
fi

supersu --stop 2>/dev/null
stop
if [ -d /debug_ramdisk ]; then
  umount -l /debug_ramdisk 2>/dev/null
fi

setprop sys.boot_completed 0

if ! grep -q ' /cache ' /proc/mounts; then
  mount -t tmpfs -o 'mode=0755' tmpfs /cache
fi

SUPERSUTMP=/sbin

if mount | grep -q rootfs; then
  mount -o rw,remount /
  rm -rf /root
  mkdir /root /sbin 2>/dev/null
  chmod 750 /root /sbin
  ln /sbin/* /root
  mount -o ro,remount /
  mount_sbin
  ln -s /root/* /sbin
elif [ -e /sbin ]; then
  mount_sbin
  mkdir -p /dev/sysroot
  block=$(mount | grep ' / ' | awk '{ print $1 }')
  [ $block = "/dev/root" ] && block=/dev/block/vda1
  mount -o ro $block /dev/sysroot
  for file in /dev/sysroot/sbin/*; do
    [ ! -e $file ] && break
    if [ -L $file ]; then
      cp -af $file /sbin
    else
      sfile=/sbin/$(basename $file)
      touch $sfile
      mount -o bind $file $sfile
    fi
  done
  umount -l /dev/sysroot
  rm -rf /dev/sysroot
else
  SUPERSUTMP=/debug_ramdisk
  mount_tmpfs /debug_ramdisk
fi

mkdir -p $SUPERSUBIN 2>/dev/null
unzip -oj supersu.apk 'assets/*.sh' -d $SUPERSUBIN
mkdir /data/adb/modules 2>/dev/null
mkdir /data/adb/post-fs-data.d 2>/dev/null
mkdir /data/adb/service.d 2>/dev/null

unzip -oj supersu.apk 'assets/chromeos/hrubin' -d ./
chmod 755 ./hrubin

for file in supersu supersu32 supersupolicy stub.apk hrubin; do
  chmod 755 ./$file
  cp -af ./$file $SUPERSUTMP/$file
  cp -af ./$file $SUPERSUBIN/$file
done
cp -af ./supersuboot $SUPERSUBIN/supersuboot
cp -af ./supersuinit $SUPERSUBIN/supersuinit
cp -af ./busybox $SUPERSUBIN/busybox

ln -s ./supersu $SUPERSUTMP/su
ln -s ./supersu $SUPERSUTMP/resetprop
ln -s ./supersupolicy $SUPERSUTMP/supolicy

mkdir -p $SUPERSUTMP/.supersu/device
mkdir -p $SUPERSUTMP/.supersu/worker
mount_tmpfs $SUPERSUTMP/.supersu/worker
mount --make-private $SUPERSUTMP/.supersu/worker
touch $SUPERSUTMP/.supersu/config

export SUPERSUTMP
MAKEDEV=1 $SUPERSUTMP/supersu --preinit-device 2>&1

RULESCMD=""
rule="$SUPERSUTMP/.supersu/preinit/sepolicy.rule"
[ -f "$rule" ] && RULESCMD="--apply $rule"

if [ -d /sys/fs/selinux ]; then
  if [ -f /vendor/etc/selinux/precompiled_sepolicy ]; then
    ./supersupolicy --load /vendor/etc/selinux/precompiled_sepolicy --live --supersu $RULESCMD 2>&1
  elif [ -f /sepolicy ]; then
    ./supersupolicy --load /sepolicy --live --supersu $RULESCMD 2>&1
  else
    ./supersupolicy --live --supersu $RULESCMD 2>&1
  fi
fi

$SUPERSUTMP/supersu --post-fs-data
start
$SUPERSUTMP/supersu --service

$SUPERSUTMP/supersu --install-module $SUPERSUBIN/hrubin
rm -f /data/adb/hrubin

sleep 2
$SUPERSUTMP/supersu --boot-complete
