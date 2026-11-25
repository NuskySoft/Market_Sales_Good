"""SQLite checks using the actual migration/DAO SQL. Does not replace Android/Room tests."""
from pathlib import Path
import re
import sqlite3
import tempfile
import unittest
from concurrent.futures import ThreadPoolExecutor

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'app/src/main/java/es/nuskysoftware/marketsales/data/local'
source = (BASE / 'database/AppDatabase.kt').read_text()
CREATE = re.search(r'CREATE TABLE IF NOT EXISTS stock_movements \(.*?\)', source, re.S).group()
queries = re.findall(r'@Query\("([^"]+)"\)', (BASE / 'dao/StockMovementDao.kt').read_text())
APPLY, ACK = queries[-1], queries[-2]
SYNC = re.search(r'@Query\("(UPDATE articulos SET sincronizadoFirebase = 1 WHERE idArticulo = :id AND userId.*?)"\)', (BASE / 'dao/ArticulosDao.kt').read_text()).group(1)

class StockSQLTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.path = Path(self.tmp.name) / 'test.db'
        with self.connect() as db:
            db.executescript('CREATE TABLE articulos(idArticulo TEXT PRIMARY KEY, userId TEXT, stock INTEGER, version INTEGER, lastModified INTEGER, sincronizadoFirebase INTEGER); CREATE TABLE recibos(id TEXT PRIMARY KEY);')
            db.execute("INSERT INTO articulos VALUES ('item','owner',10,1,1,1)")
            db.execute(CREATE)
    def tearDown(self):
        self.tmp.cleanup()
    def connect(self):
        return sqlite3.connect(self.path, timeout=10)
    def sale(self, identifier='sale'):
        with self.connect() as db:
            db.execute('INSERT INTO recibos VALUES (?)', (identifier,))
            db.execute('INSERT INTO stock_movements VALUES (?,?,?,?,?,?,?)', (identifier,'owner',identifier,'item',2,1,0))
    def process(self):
        with self.connect() as db:
            db.execute('BEGIN IMMEDIATE')
            for row in db.execute('SELECT idMovimiento,idArticulo,idUsuario,cantidad FROM stock_movements WHERE aplicado=0').fetchall():
                movement, article, owner, quantity = row
                assert db.execute(APPLY, dict(articleId=article,owner=owner,quantity=quantity,now=2)).rowcount == 1
                db.execute(ACK, dict(id=movement))
    def stock(self):
        with self.connect() as db:
            return db.execute('SELECT stock FROM articulos').fetchone()[0]
    def test_failure_rolls_back_stock_only_and_reopening_recovers_once(self):
        self.sale()
        with self.connect() as db:
            db.executescript("CREATE TRIGGER fail_ack BEFORE UPDATE OF aplicado ON stock_movements BEGIN SELECT RAISE(ABORT,'failure'); END;")
        with self.assertRaises(sqlite3.IntegrityError):
            self.process()
        self.assertEqual(self.stock(),10)
        with self.connect() as db:
            self.assertEqual(db.execute('SELECT COUNT(*) FROM recibos').fetchone()[0],1)
            db.execute('DROP TRIGGER fail_ack')
        self.process(); self.process()
        self.assertEqual(self.stock(),8)
    def test_concurrent_retries_apply_each_movement_once(self):
        self.sale('a'); self.sale('b')
        with ThreadPoolExecutor(max_workers=6) as pool:
            list(pool.map(lambda _: self.process(), range(12)))
        self.assertEqual(self.stock(),6)
    def test_wrong_owner_remains_pending(self):
        self.sale()
        with self.connect() as db:
            db.execute("UPDATE articulos SET userId='other'")
        with self.assertRaises(AssertionError):
            self.process()
        self.assertEqual(self.stock(),10)
        with self.connect() as db:
            self.assertEqual(db.execute('SELECT aplicado FROM stock_movements').fetchone()[0],0)
    def test_upload_ack_does_not_clear_new_stock_change(self):
        self.sale(); self.process()
        with self.connect() as db:
            db.execute(SYNC,dict(id='item',owner='owner',version=1,modified=1,stock=10))
            self.assertEqual(db.execute('SELECT sincronizadoFirebase FROM articulos').fetchone()[0],0)
            db.execute(SYNC,dict(id='item',owner='owner',version=2,modified=2,stock=8))
            self.assertEqual(db.execute('SELECT sincronizadoFirebase FROM articulos').fetchone()[0],1)
    def test_additive_migration_keeps_existing_stock_and_sales(self):
        with self.connect() as db:
            db.execute('DROP TABLE stock_movements')
            db.execute("INSERT INTO recibos VALUES ('historical')")
            db.execute(CREATE)
            self.assertEqual(db.execute('SELECT COUNT(*) FROM stock_movements').fetchone()[0],0)
            self.assertEqual(db.execute('SELECT COUNT(*) FROM recibos').fetchone()[0],1)
        self.assertEqual(self.stock(),10)

if __name__ == '__main__':
    unittest.main(verbosity=2)
