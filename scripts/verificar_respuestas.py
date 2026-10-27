"""Captura respuestas reales sin exportar tokens. Ejecutar con AgroAPI iniciado."""
import json
import urllib.request
import urllib.error
from pathlib import Path
from datetime import datetime, timezone

BASE = 'http://localhost:8080/api/v1'
def consultar(method, path, body=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token: headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE + path, method=method, headers=headers,
        data=None if body is None else json.dumps(body).encode())
    try: response = urllib.request.urlopen(req)
    except urllib.error.HTTPError as e: response = e
    raw = response.read().decode()
    return response.status, json.loads(raw) if raw else None

if __name__ == '__main__':
    tokens = {}
    for u in ['tecnico1', 'jefe1', 'admin']:
        status, body = consultar('POST', '/auth/login', {'usuario':u, 'contrasena':'Agro2026*'})
        assert status == 200
        tokens[u] = body['token']
    labor = {'loteId':2,'tipo':'FUMIGACION','fecha':'2026-10-27','cantidad':9999,'observacion':'Prueba de validación'}
    casos = [('401 sin token','GET','/lotes',None,None,401),
        ('403 administrador no registra','POST','/labores',labor,tokens['admin'],403),
        ('400 dosis excesiva','POST','/labores',labor,tokens['tecnico1'],400),
        ('409 sin cosecha','POST','/lotes/2/cerrar-campania',None,tokens['jefe1'],409)]
    resultados = []
    for nombre, method, path, data, token, esperado in casos:
        status, body = consultar(method, path, data, token)
        assert status == esperado, (nombre,status)
        resultados.append({'caso':nombre,'metodo':method,'ruta':path,'status':status,'respuesta':body})
    _, pagina = consultar('GET','/lotes/2/labores?pagina=0&tamanio=100',token=tokens['tecnico1'])
    offline = [l for l in pagina['contenido'] if l['observacion']=='Evidencia_offline_27oct']
    reporte = {'fecha':datetime.now(timezone.utc).isoformat(),'respuestas':resultados,
        'registroOffline':{'coincidencias':len(offline),'ids':[l['id'] for l in offline]}}
    destino = Path(__file__).resolve().parents[1] / 'evidencias' / 'respuestas-reales.json'
    destino.write_text(json.dumps(reporte,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({'casos':len(resultados),'registroOffline':reporte['registroOffline']},ensure_ascii=False))
