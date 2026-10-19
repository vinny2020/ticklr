#!/usr/bin/env python3
"""Capture all native tutorial examples on the isolated TicklrTutorials emulator.

Build/install the debug and androidTest APKs first. No physical devices, contact
permissions, personal data, or pixel editing are used. The capture test seeds
bundled fake contacts only when the isolated emulator has none.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

ROOT=Path(__file__).resolve().parents[1]
LANGUAGES={'en':'en-US','ar':'ar','cs':'cs-CZ','de':'de-DE','el':'el-GR','es':'es-ES','fr':'fr-FR','he':'he','hi':'hi-IN','hu':'hu-HU','it':'it-IT','ja':'ja-JP','ko':'ko-KR','nl':'nl-NL','pl':'pl-PL','pt':'pt-PT','ro':'ro-RO','ru':'ru-RU','sv':'sv-SE','ur':'ur','zh-Hans':'zh-Hans'}
SCREENS=['import','network','profile','tickle','birthday','membership','groups','new_group']

def main():
    p=argparse.ArgumentParser();p.add_argument('--adb',default=str(Path.home()/'Library/Android/sdk/platform-tools/adb'));p.add_argument('--serial',default='emulator-5554');p.add_argument('--languages',nargs='*',default=list(LANGUAGES));p.add_argument('--output',type=Path,default=Path('/private/tmp/ticklr-native-captures'));args=p.parse_args()
    def adb(*cmd):
        return subprocess.run([args.adb,'-s',args.serial,*cmd],check=True,capture_output=True,text=True,timeout=180).stdout
    if not args.serial.startswith('emulator-') or adb('shell','getprop','ro.kernel.qemu').strip()!='1' or adb('emu','avd','name').splitlines()[0]!='TicklrTutorials':
        raise SystemExit('Only the isolated TicklrTutorials emulator is allowed')
    res=ROOT/'android/app/src/main/res'
    for lang in args.languages:
        locale=LANGUAGES[lang]
        adb('shell','am','force-stop','com.xaymaca.sit.debug')
        adb('shell','cmd','locale','set-app-locales','com.xaymaca.sit.debug','--user','0','--locales',locale)
        result=adb('shell','am','instrument','-w','-r','-e','class','com.xaymaca.sit.TutorialScreenshotCaptureTest','-e','captureTutorialScreenshots','true','-e','locale',locale,'com.xaymaca.sit.debug.test/androidx.test.runner.AndroidJUnitRunner')
        dest=args.output/locale;dest.mkdir(parents=True,exist_ok=True)
        (dest/'capture.log').write_text(result)
        if 'OK (1 test)' not in result:
            raise SystemExit(f'{locale}: capture failed; see {dest}/capture.log')
        qualifier={'zh-Hans':'zh'}.get(lang,lang)
        folders=['drawable-nodpi'] if lang=='en' else ['drawable-'+qualifier+'-nodpi']
        if lang=='he':folders.append('drawable-iw-nodpi')
        for screen in SCREENS:
            name='tutorial_android_'+screen+'.png'
            local=dest/name
            adb('pull',f'/sdcard/Android/data/com.xaymaca.sit.debug/files/tutorials/{locale}/{name}',str(local))
            if local.read_bytes()[:8] != b'\x89PNG\r\n\x1a\n':raise SystemExit(f'Invalid PNG: {local}')
            for folder in folders:
                (res/folder).mkdir(exist_ok=True);shutil.copy2(local,res/folder/name)
        print(f'{locale}: captured and installed {len(SCREENS)} native screenshots',flush=True)
    # Record hashes for every locale so missing or stale copied assets are visible.
    manifest={}
    for lang in LANGUAGES:
        q={'zh-Hans':'zh','en':''}.get(lang,lang)
        folder=res/('drawable-nodpi' if lang=='en' else 'drawable-'+q+'-nodpi')
        if all((folder/('tutorial_android_'+s+'.png')).exists() for s in SCREENS):
            manifest[lang]={s:hashlib.sha256((folder/('tutorial_android_'+s+'.png')).read_bytes()).hexdigest() for s in SCREENS}
    (ROOT/'scripts/tutorial-localization/screenshots.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(f'{len(manifest)} language image sets available',flush=True)

if __name__=='__main__':main()
