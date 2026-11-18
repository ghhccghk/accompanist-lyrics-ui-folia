"""Record public integration, not upstream visual equivalence or device results."""
from pathlib import Path
import hashlib,json,shutil,xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[2];out=Path(__file__).resolve().parent
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
suites=[]
for module,names in [('library',['PublicDioramaTest','DioramaTextPlaybackTest','DioramaTextTest','DioramaPreparationCancellationTest']),
                     ('sample/shared',['ThemeOptionsTest','SettingsTest'])]:
    for name in names:
        matches=list((root/module/'build/test-results/jvmTest').glob('TEST-*.'+name+'.xml'))
        assert len(matches)==1
        suite=ET.parse(matches[0]).getroot()
        assert suite.attrib['failures']=='0' and suite.attrib['errors']=='0'
        suites.append(dict(module=module,suite=name,tests=int(suite.attrib['tests']),failures=0,errors=0,
            timestamp=suite.attrib['timestamp']))
log=(root/'.work/diorama-preview-final.log').read_text(encoding='utf8',errors='replace')
assert 'BUILD SUCCESSFUL' in log
images=[]
for name in ['public-800.png','public-380.png']:
    p=root/'build/verification/diorama-public'/name;target=out/name
    if target.exists():assert sha(p)==sha(target),'Do not replace previous screenshot'
    else:shutil.copy2(p,target)
    images.append(dict(file=name,sha256=sha(p)))
files=['FoliaOptions.kt','FoliaLyricsView.kt']+[
    'internal/styles/diorama/'+n for n in ['DioramaPlaybackStage.kt','DioramaTextHitTest.kt','DioramaTextPlayback.kt',
        'DioramaActiveText.kt','DioramaPlainText.kt']]
base=root/'library/src/commonMain/kotlin/top/ghhccghk/accompanist/lyrics/folia'
report=dict(baseline='b051bcc',implementationHashes={n:sha(base/n) for n in files},
    sourceRevision='cc6226e464050a456a2913e062374f23d3844cc5',
    conditions=dict(font='verification/assets/FoliaParityLatin-Bold.ttf',fontSha256=sha(root/'verification/assets/FoliaParityLatin-Bold.ttf'),
        root=[800,600],stageWidths=[800,380],density=1,mediaMillis=1600,offsetEquivalentMediaMillis=1800,offsetMillis=200,
        seed='Diorama',background='#162435',source='Two original synthetic lyrics-core SyncedLine objects, 700..2600 and 3100..4600 ms'),
    suites=suites,images=images,
    assertions=['public and internal host identical paused pixel arrays','true source-object click/long press',
        'inverse actual perspective/alpha blank fallthrough','paused frame stable',
        'forward/back seek and offset equivalence','380/800 resize restore','system font scale restore',
        'budget bypass rebuild preserves in-budget pixels','budget default enabled and disabled limit/edge bypass',
        'oversize error then replacement recovery','theme switch/remount and equal-value new-object refs',
        'preparation cancellation inside shaping/raster/halo/core','all eight sample entries preserve playback',
        'budget UI toggle on both widths and preference roundtrip/default compatibility'],
    priorFailures=['initial-failure.json','second-failure.json','budget-invalidation-failure.json'],
    androidLibraryAndSharedCompiled=True,desktopCompiled=True,androidDeviceRun=False,
    realDesktopWindow=False,upstreamFullVisualAcceptance=False,performanceMeasured=False,
    publicPreview='Experimental text perspective; Android ordinary fallback. No full depth/sequencer parity.')
(out/'validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps(dict(tests=sum(s['tests'] for s in suites),images=images),indent=2))
