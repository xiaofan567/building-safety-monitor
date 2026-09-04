import zipfile
import xml.etree.ElementTree as ET
import sys

src = r'C:\Users\29500\xwechat_files\wxid_blkvraq3eqg922_ae68\msg\file\2026-09\系统接口设计文档（完整版汇总）.docx'
dst = r'd:\javatools\ai_practice\building-safety-monitor\smartbuildings-backend\doc_output.txt'

try:
    z = zipfile.ZipFile(src)
    xml = z.read('word/document.xml')
    root = ET.fromstring(xml)
    ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
    
    with open(dst, 'w', encoding='utf-8') as f:
        count = 0
        for p in root.findall('.//w:p', ns):
            text = ''.join(t.text or '' for t in p.findall('.//w:t', ns))
            f.write(text + '\n')
            count += 1
    
    sys.stdout.write('DONE:' + str(count))
    sys.stdout.flush()
except Exception as e:
    sys.stdout.write('ERROR:' + str(e))
    sys.stdout.flush()