import xml.etree.ElementTree as ET
try:
    for f in ['build/reports/checkstyle/main.xml', 'build/reports/checkstyle/test.xml']:
        tree = ET.parse(f)
        for f_node in tree.findall('.//file'):
            filename = f_node.get('name')
            for e in f_node.findall('.//error'):
                if 'ConstantName' in e.get('source') or 'LocalFinalVariableName' in e.get('source'):
                    msg = e.get('message')
                    name = msg.split("Name '")[1].split("'")[0]
                    print(f"{filename}:{e.get('line')} - {name}")
except Exception as err:
    print('Failed', err)
