from sqlalchemy import create_engine as sqlalchemy_create_engine
from sqlalchemy.orm import declarative_base
from sqlalchemy import Column, Integer, String, Sequence, ForeignKey, Float, DateTime
    
# psql -d transformer -U postgres --password  # postgres
def get_db_uri(host='127.0.0.1') -> str:
    user = 'postgres'
    pw = 'postgres'
    # host = '127.0.0.1' # 'postgres'
    port = 5432
    dbname = 'transformer'
    return f'postgresql+psycopg2://{user}:{pw}@{host}:{port}/{dbname}'

engine = None
# engine = create_engine(get_db_uri(host), pool_size=20, pool_pre_ping=True)

Base = declarative_base()

class Apk(Base):
    __tablename__ = 'apks'

    id = Column(Integer, Sequence('user_id_seq', start=1, increment=1), primary_key=True)

    package_name = Column(String, nullable=False)
    file_path = Column(String, nullable=False, unique=True)
    original_dimension = Column(Integer, nullable=True)
    
class Protection(Base):
    __tablename__ = 'protections'

    id = Column(Integer, Sequence('user_id_seq', start=1, increment=1), primary_key=True)

    apk_id = Column(Integer, ForeignKey('apks.id'), nullable=False)

    chance_extractor = Column(Integer, nullable=False)
    chance_encryptor = Column(Integer, nullable=False)
    chance_injector = Column(Integer, nullable=False)

    n_extracted_method = Column(Integer, nullable=True) # Methods involved in code-splitting
    n_at_checks = Column(Integer, nullable=True) # Methods protected with base IATs
    n_encrypted_methods = Column(Integer, nullable=True) # Methods protected with IAT with encryption

    final_dimension = Column(Integer, nullable=True)
    error = Column(String, nullable=True) # The stack-trace of any error that occurs
    execution_time = Column(Float, nullable=False)

    from datetime import datetime
    datetime = Column(DateTime, default=datetime.utcnow)

# Base.metadata.create_all(engine)
def create_engine(host='127.0.01'):
    global engine

    if engine is None:
        engine = sqlalchemy_create_engine(get_db_uri(host), pool_size=20, pool_pre_ping=True)
        Base.metadata.create_all(engine)
        return engine
    else:
        return engine
