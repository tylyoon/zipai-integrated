import importlib.util,unittest,tempfile
from pathlib import Path
spec=importlib.util.spec_from_file_location("generator",Path(__file__).resolve().parents[1]/"tools/generate_presentation_audio.py")
g=importlib.util.module_from_spec(spec);spec.loader.exec_module(g)
class Tests(unittest.TestCase):
 def test_audio_validation(self):
  self.assertTrue(g.valid_mp3(b"ID3"+b"x"*1001));self.assertFalse(g.valid_mp3(b'{"error":"unauthorized"}'))
 def test_voice_filename(self):
  self.assertEqual(g.filename("안녕하세요"),g.filename("안녕하세요"));self.assertNotEqual(g.filename("안녕하세요"),g.filename("감사합니다"));self.assertTrue(g.filename("test").startswith("samantha-"))
 def test_manifest_unicode(self):
  with tempfile.TemporaryDirectory() as d:
   old=g.MANIFEST;g.MANIFEST=Path(d)/"manifest.js"
   try:
    g.write_manifest({"안녕하세요":"/static/audio/presentation/test.mp3"})
    self.assertIn("안녕하세요",g.MANIFEST.read_text());self.assertFalse(g.MANIFEST.with_suffix(".tmp").exists())
   finally:g.MANIFEST=old
if __name__=="__main__":unittest.main()
