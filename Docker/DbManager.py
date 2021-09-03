from sqlalchemy.orm import sessionmaker
from sqlalchemy.exc import IntegrityError
from sqlalchemy import func

class DBManager:
    def __init__(self, connection):
        self.connection = connection
        Session = sessionmaker(bind=connection, expire_on_commit=False)
        self.session = Session()

    def insert(self, obj):
        try:
            self.session.add(obj)
            self.session.commit()
        except Exception:
            self.session.rollback()
            raise

    def insert_apk(self, path, package_name):
        import os
        dimension = os.path.getsize(path)

        from models import Apk
        apk = Apk(package_name=package_name, original_dimension=dimension, file_path=path) # TODO: handle different paths
        try:
            self.insert(apk)
            return False, apk.id
        except IntegrityError as e:
            print(f"[-] Apk {path} already present in db")
            pass

        #if not apk.id or apk.id is None: # possible exception due to package already present
        # Retrieve apk with this 
        apk = self.session.query(Apk).filter(Apk.file_path == path).one()
        return True, apk.id

    def new_protection(self, apk_id, chance_extractor, chance_encryptor, chance_injector, execution_time, error=None, n_extracted_method=-1,
        n_at_checks=-1, n_encrypted_methods=-1, final_dimension=-1, file_path=None):
        from models import Protection
        protection = Protection(apk_id=apk_id, chance_extractor=chance_extractor, chance_encryptor=chance_encryptor,
            chance_injector=chance_injector, error=error, n_extracted_method=n_extracted_method, n_at_checks=n_at_checks, 
            n_encrypted_methods=n_encrypted_methods, final_dimension=final_dimension, execution_time=execution_time)
        self.insert(protection)
        return protection.id

# from models import engine
# db_manager = DBManager(engine)