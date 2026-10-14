"""Typecast public narration. Key stays in memory; no .env/key files loaded."""
import getpass,hashlib,json,sys,urllib.request,urllib.error
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
VOICE="tc_62baac538f06bd484ee976bd"
MODEL="ssfm-v30"
OUT=ROOT/"static/audio/presentation"
MANIFEST=ROOT/"static/js/presentation-audio-manifest.js"
def valid_mp3(body):
    return len(body)>1000 and (body.startswith(b"ID3") or (body[0]==255 and body[1]&224==224))
def filename(text):
    return "samantha-"+hashlib.sha256((VOICE+MODEL+text).encode()).hexdigest()[:20]+".mp3"
def write_manifest(mapping):
    MANIFEST.parent.mkdir(parents=True,exist_ok=True)
    temp=MANIFEST.with_suffix(".tmp")
    temp.write_text("window.ZIPAI_AUDIO_MANIFEST = "+json.dumps(mapping,ensure_ascii=False)+";\n",encoding="utf-8")
    temp.replace(MANIFEST)
def generate(text,key):
    payload={"voice_id":VOICE,"text":text,"model":MODEL,"language":"kor","output":{"audio_format":"mp3","audio_tempo":1.0}}
    request=urllib.request.Request("https://api.typecast.ai/v1/text-to-speech",data=json.dumps(payload,ensure_ascii=False).encode(),headers={"X-API-KEY":key,"Content-Type":"application/json"},method="POST")
    with urllib.request.urlopen(request,timeout=90) as response:
        body=response.read(20*1024*1024)
    if not valid_mp3(body):raise ValueError("Invalid MP3 response")
    return body
def main():
    texts=list(dict.fromkeys(json.loads((ROOT/"tools/presentation-narration.json").read_text(encoding="utf-8"))))
    if not all(isinstance(t,str) and 0<len(t)<=2000 for t in texts):
        print("대본 형식 또는 API 글자 제한 확인 필요. 중단합니다.");return 1
    OUT.mkdir(parents=True,exist_ok=True)
    mapping={};pending=[]
    for text in texts:
        target=OUT/filename(text)
        if target.exists() and valid_mp3(target.read_bytes()):mapping[text]="/static/audio/presentation/"+target.name
        else:pending.append(text)
    cost=sum(map(len,pending))
    print(f"사만다: 전체 {len(texts)}개 / 기존 재사용 {len(mapping)}개 / 새 생성 {len(pending)}개 / 예상 {cost}크레딧")
    if not pending:
        write_manifest(mapping);print("전체 음성 생성 완료. 새 API 호출 없음.");return 0
    if not sys.stdin.isatty():print("make_voice.bat을 더블클릭하세요.");return 1
    try:
        remaining=int(input("Typecast 구독 관리의 현재 잔여 크레딧 숫자 입력: ").replace(",","").strip())
    except ValueError:print("숫자가 아니어서 중단합니다.");return 1
    if cost>remaining:
        print("무료 잔여량보다 생성량이 많습니다. 요청하지 않습니다.");return 1
    if input("위 분량을 생성하려면 Y 입력: ").strip().lower()!="y":return 0
    key=getpass.getpass("Typecast API 키 붙여넣기 (표시·파일 저장 안 됨): ").strip()
    if not key:print("키가 비어 있어 중단합니다.");return 1
    write_manifest(mapping)
    for number,text in enumerate(texts,1):
        if text in mapping:
            print(f"{number}/{len(texts)} REUSE",flush=True);continue
        try:
            body=generate(text,key)
            target=OUT/filename(text);temp=target.with_suffix(".part")
            temp.write_bytes(body);temp.replace(target)
            mapping[text]="/static/audio/presentation/"+target.name
            write_manifest(mapping)
            print(f"{number}/{len(texts)} OK",flush=True)
        except urllib.error.HTTPError as error:
            print(f"HTTP {error.code}: 생성 중단. API 키·목소리 권한·잔여량을 확인하세요. 자동 재시도하지 않습니다.")
            print(f"완료 {len(mapping)}/{len(texts)}. 다음 실행 시 완료 파일을 재사용합니다.")
            return 1
        except (urllib.error.URLError,TimeoutError,ValueError,OSError):
            print(f"{number}/{len(texts)} 연결·응답·파일 저장 실패. 중단합니다. 완료 {len(mapping)}개 재사용 가능.")
            return 1
    print(f"전체 음성 생성 완료: {len(mapping)}/{len(texts)}")
    print("MP3와 연결 목록을 함께 Git에 올려 배포하세요. API 키는 저장하지 않았습니다.")
    return 0
if __name__=="__main__":sys.exit(main())
