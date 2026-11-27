from pathlib import Path
import re, sqlite3, subprocess, sys
root=Path(__file__).resolve().parents[1] if (Path(__file__).resolve().parent.name=='scripts') else Path('/workspace/scratch/ec276545d59f/market_24')
base=root/'app/src/main/java/es/nuskysoftware/marketsales'
db=sqlite3.connect(':memory:')
tables={}
for file in (base/'data/local/entity').glob('*.kt'):
 text=file.read_text();found=re.search(r'@Entity\([\s\S]*?tableName\s*=\s*"([^"]+)"',text)
 if not found: continue
 table=found.group(1);data=text[text.index('data class '):];data=data.split('// Constructor')[0]
 fields=[]
 for name,kind,nullable in re.findall(r'\bval\s+(\w+):\s*(String|Int|Long|Double|Boolean)(\?)?',data):
  if name in [x[0] for x in fields]:continue
  sqltype='TEXT' if kind=='String' else 'REAL' if kind=='Double' else 'INTEGER'
  fields.append((name,sqltype,nullable,kind))
 tables[table]=fields
 db.execute('CREATE TABLE "'+table+'" ('+', '.join('"'+n+'" '+t for n,t,_,_ in fields)+')')
# Extra private migration table isn't a managed entity.
count=0;errors=[]
for file in (base/'data/local/dao').glob('*.kt'):
 text=file.read_text()
 for m in re.finditer(r'(?m)^[ \t]*@Query\(("""[\s\S]*?"""|"[^"\n]*")\)',text):
  q=m.group(1);q=q[3:-3] if q.startswith('"""') else q[1:-1]
  args={name:0 for name in re.findall(r':(\w+)',q)}
  try: db.execute('EXPLAIN '+q,args).fetchall();count+=1
  except Exception as e:errors.append((file.name,q,str(e)))
if errors:
 for error in errors:print(error)
 sys.exit(1)
print(f'PASS: {count} actual Room queries prepared against all entity columns.')
# Dates: range/order must cross month and year chronologically.
for uid,mid,date,day in [('a','a1','30-11-2026',20787),('a','a2','02-12-2026',20789),('b','b1','01-01-2027',20819)]:
 db.execute('INSERT INTO mercadillos(userId,idMercadillo,fecha,fechaEpochDay,activo,estado) VALUES (?,?,?,?,1,1)',(uid,mid,date,day))
assert [x[0] for x in db.execute('SELECT idMercadillo FROM mercadillos WHERE userId=? ORDER BY fechaEpochDay',('a',))]==['a1','a2']
# Account-scoped writes cannot confirm another owner's expense even with identical line number.
db.execute("INSERT INTO LineasGastos(idUsuario,idMercadillo,numeroLinea,lastModified,version,sincronizadoFirebase,importe) VALUES ('a','a1','0001',10,1,0,1.23),('b','b1','0001',10,1,0,4.56)")
q=re.search(r'@Query\("(UPDATE LineasGastos SET sincronizadoFirebase = 1[^"\n]+)"',(base/'data/local/dao/LineasGastosDao.kt').read_text()).group(1)
db.execute(q,dict(uid='a',mercadillo='a1',numeroLinea='0001',timestamp=10,version=1))
assert db.execute("SELECT sincronizadoFirebase FROM LineasGastos WHERE idUsuario='b'").fetchone()[0]==0
# New edit while an old snapshot is uploading stays pending.
db.execute("UPDATE LineasGastos SET lastModified=11,sincronizadoFirebase=0 WHERE idUsuario='a'")
db.execute(q,dict(uid='a',mercadillo='a1',numeroLinea='0001',timestamp=10,version=1))
assert db.execute("SELECT sincronizadoFirebase FROM LineasGastos WHERE idUsuario='a'").fetchone()[0]==0
# Union of independent stock deltas, not last-writer-wins quantities; exact-once processing.
db.execute("INSERT INTO articulos(idArticulo,userId,stock,stockBase) VALUES ('p','a',10,10)")
for mid,qty in [('s1',2),('s2',3)]:db.execute('INSERT INTO stock_movements(idMovimiento,idUsuario,idArticulo,cantidad,aplicado,incorporadoEnBase) VALUES (?,\'a\',\'p\',?,1,0)',(mid,qty))
stocksql=re.search(r'sql.execSQL\("(UPDATE articulos SET stock = stockBase[^"\n]+)"',(base/'data/repository/SyncEngine.kt').read_text()).group(1)
for _ in range(2):db.execute(stocksql,('a',))
assert db.execute("SELECT stock FROM articulos WHERE idArticulo='p'").fetchone()[0]==5
# Multiple independent balances; consuming one doesn't delete or consume another.
db.execute("INSERT INTO saldos_guardados(idRegistro,idUsuario,consumido,eliminado) VALUES ('s1','a',0,0),('s2','a',0,0)")
db.execute("UPDATE saldos_guardados SET consumido=1 WHERE idRegistro='s1' AND idUsuario='a'")
assert db.execute("SELECT idRegistro FROM saldos_guardados WHERE idUsuario='a' AND consumido=0 AND eliminado=0").fetchall()==[('s2',)]
print('PASS: chronological/account-scoped queries, snapshot acknowledgement, additive stock and multiple balances.')
# Execute the actual 19 -> 20 SQL strings against a populated v19 fixture.
migration=(base/'data/local/database/AppDatabase.kt').read_text().split('val MIGRATION_19_20',1)[1].split('// Additive migration.',1)[0]
old=sqlite3.connect(':memory:')
old.executescript('''
CREATE TABLE configuracion(id INTEGER, usuarioLogueado TEXT);
INSERT INTO configuracion VALUES(1,'a');
CREATE TABLE empresa(id INTEGER,nif TEXT,nombre TEXT,razonSocial TEXT,direccion TEXT,poblacion TEXT,codigoPostal TEXT,provincia TEXT,pais TEXT,version INTEGER,lastModified INTEGER,sincronizadoFirebase INTEGER);
INSERT INTO empresa VALUES(1,'NIF','Empresa A','','','','','','',1,10,1);
CREATE TABLE articulos(idArticulo TEXT,userId TEXT,stock INTEGER);
INSERT INTO articulos VALUES('p','a',8);
CREATE TABLE stock_movements(idMovimiento TEXT,idUsuario TEXT,idRecibo TEXT,idArticulo TEXT,cantidad INTEGER,creadoEn INTEGER,aplicado INTEGER);
INSERT INTO stock_movements VALUES('old-sale','a','r','p',2,1,1);
CREATE TABLE saldos_guardados(idRegistro TEXT,fechaMercadillo TEXT);
INSERT INTO saldos_guardados VALUES('s','30-11-2026');
CREATE TABLE mercadillos(idMercadillo TEXT,fecha TEXT);
INSERT INTO mercadillos VALUES('m','02-12-2026');
CREATE TABLE recibos(idRecibo TEXT); INSERT INTO recibos VALUES('r');
''')
for sql in re.findall(r'db.execSQL\("([^"\n]+)"\)',migration):
 if '$' not in sql:old.execute(sql)
assert old.execute("SELECT userId,nombre FROM empresa").fetchall()==[('a','Empresa A')]
assert old.execute("SELECT stock,stockBase FROM articulos").fetchall()==[(8,8)]
assert old.execute("SELECT incorporadoEnBase,sincronizadoFirebase FROM stock_movements").fetchall()==[(1,1)]
assert old.execute('SELECT COUNT(*) FROM recibos').fetchone()[0]==1
print('PASS: actual migration SQL preserves company ownership, current stock, historical ledger and receipts.')
# Exercise generated money/date trigger statements with the same callback template.
monetary={'articulos':['precioVenta','precioCoste'],'mercadillos':['importeSuscripcion','saldoInicial','saldoFinal','arqueoCaja','totalVentas','totalGastos','arqueoMercadillo'],'recibos':['totalTicket'],'lineas_venta':['precioUnitario','subtotal'],'LineasGastos':['importe'],'saldos_guardados':['saldoInicialGuardado']}
for table,cols in monetary.items():
 for event in ['INSERT','UPDATE']:
  condition=' OR '.join(f'NEW.{c} != (ROUND(NEW.{c} * 100,0) / 100.0)' for c in cols)
  assign=', '.join(f'{c}=(ROUND(NEW.{c} * 100,0) / 100.0)' for c in cols)
  db.execute(f'CREATE TRIGGER money_{table}_{event} AFTER {event} ON {table} WHEN {condition} BEGIN UPDATE {table} SET {assign} WHERE rowid=NEW.rowid; END')
db.execute("UPDATE articulos SET precioVenta=1.235,precioCoste=0.105 WHERE idArticulo='p'")
assert db.execute("SELECT precioVenta,precioCoste FROM articulos WHERE idArticulo='p'").fetchone()==(1.24,0.11)
print('PASS: persistence guards normalize monetary amounts to two decimals.')
# Room rejects unused DAO arguments as well as missing bind parameters.
for file in (base/'data/local/dao').glob('*.kt'):
    source=file.read_text()
    for match in re.finditer(r'(?m)^[ \t]*@Query\(("""[\s\S]*?"""|"[^"\n]*")\)',source):
        query=match.group(1)
        method=re.match(r'\s*(?:suspend\s+)?fun\s+\w+\s*\(',source[match.end():])
        if not method: continue
        start=match.end()+method.end(); end=start; depth=1
        while depth:
            if source[end]=='(':depth+=1
            elif source[end]==')':depth-=1
            end+=1
        arguments=set(re.findall(r'(?:^|,)\s*(\w+)\s*:',source[start:end-1]))
        bindings=set(re.findall(r':(\w+)',query))
        assert arguments==bindings, (file.name,arguments,bindings)
print('PASS: all DAO arguments match SQL bind parameters; no unused arguments.')
