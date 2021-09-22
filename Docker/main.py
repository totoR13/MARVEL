from time import sleep

from sqlalchemy.engine import create_engine
sleep(5) # Wait start the docker postgres container

from argparse import ArgumentParser
from glob import glob
from subprocess import run, PIPE, CalledProcessError, TimeoutExpired
from time import time
from DbManager import DBManager
from models import create_engine
from re import search, IGNORECASE
from androguard.core.bytecodes import apk
from multiprocessing import Pool, Lock
import string
import random
from shutil import rmtree
from os import makedirs, sep, path
# from itertools import permutations

lock = None
db_manager = None

def parse_args():
    parser = ArgumentParser(description="Run the MARVELoid analysis")

    parser.add_argument('apk_folder', metavar='APK_FOLDER', type=str, help='The path of the input apks')
    parser.add_argument('-o', '--output_folder', dest='output_folder', type=str, required=False, default="/workdir/output", help='The folder containing all the output results. The default value is `/workdir/output`')
    parser.add_argument('-k', '--keep_output_files', dest='keep_output_files', action='store_true', required=False, default=False, help='Flag to remove or not the protected apks and the protection metadata.')
    parser.add_argument('-p', '--processes', dest='processes', type=int, required=False, default=2, help='The number of processes. The default value is `4`')
    parser.add_argument('--host', dest='host', type=str, required=False, default="127.0.0.1", help='The host name of the postgresql db. The default value is \'127.0.0.1\'')
    parser.add_argument('--permutations', dest='permutations', type=str, required=False, default='5,5,5', help='A \';\' separated list of permutation, a tuple in the form \'p1,p2,p3\' of 3 percentage values for the MARVELoid tool, where p1 is the change for the code splitting, p2 is a chance for the IAT with encryption and p3 is a chance for the base IAT. The default value is \'5,5,5\'')

    return parser.parse_args()

def get_apk_list(apk_folder: str) -> list:
    apks = glob(f'{apk_folder}{sep}*.apk') # TODO: check if I loose some files
    return list(apks)

def random_string(size=5, chars=string.ascii_letters + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))

def run_shell_command(cmd: str):
    try:
        timeout = 30 * 60 # 30 minuts in seconds
        result = run(cmd, shell=True, check=True, timeout=timeout, stdout=PIPE, stderr=PIPE)
        return True, result.stdout.decode()
    except CalledProcessError as e:
        return False, e.stderr.decode()
    except TimeoutExpired:
        return False, "Timeout expired"

def protect_apk(apk_path, chance_extractor, chance_encryptor, chance_injector, output_folder, keep_output_files=False):
    global lock, db_manager

    print(f"[*] Protecting app {apk_path} with --extractor-chance {chance_extractor} --encryption-chance {chance_encryptor} --injection-chance {chance_injector}")
    try:
        package_name = (apk.APK(apk_path)).package
        lock.acquire()
        _, apk_id = db_manager.insert_apk(apk_path, package_name)
        lock.release()

        apk_output_folder = f"{output_folder}/{package_name}_{chance_extractor}_{chance_encryptor}_{chance_injector}_{random_string(4)}"
        makedirs(apk_output_folder)
        print(f"[*] Creating output folder {apk_output_folder}")
        
        # TODO: update paths of android jars and keystore
        cmd = f"java -jar /jars/transformer.jar -i {apk_path} -o {apk_output_folder} -a /jars/resources/platforms --keystore-path /keytools/my-release-key.keystore --keystore-pass \"test123\" --run-injector \"true\" --run-protector \"true\" --extractor-chance {chance_extractor} --encryption-chance {chance_encryptor} --injection-chance {chance_injector} -j \"/usr/bin/jarsigner\""
        start = time()
        success, message = run_shell_command(cmd)
        execution_time = time() - start

        if success:
            print(f"[*] App {apk_path} protected successfully")
            # parse injected values
            n_extracted_method = int(search('Number of extracted methods: (.*);', message, IGNORECASE).group(1))
            n_encrypted_methods = int(search('Number of encrypted methods: (.*);', message, IGNORECASE).group(1))
            n_at_checks = int(search('Number of injected AT controls: (.*);', message, IGNORECASE).group(1))

            name = apk_path.split("/")[-1]
            output_path = f"{apk_output_folder}/{name}"
            final_dimension = path.getsize(output_path)

            lock.acquire()
            db_manager.new_protection(apk_id, chance_extractor=chance_extractor, chance_encryptor=chance_encryptor, chance_injector=chance_injector, execution_time=execution_time, 
                final_dimension=final_dimension, n_extracted_method=n_extracted_method, n_at_checks=n_at_checks, n_encrypted_methods=n_encrypted_methods)
            lock.release()

        else:
            print(f"[-] Protecting app {apk_path} fail. Error : {message}")
            lock.acquire()
            db_manager.new_protection(apk_id, chance_extractor, chance_encryptor, chance_injector, execution_time, error=message) 
            lock.release()

        # Remove folder
        if not keep_output_files:
            print(f"[-] Removing output file {apk_output_folder}")
            rmtree(apk_output_folder)

    except Exception as e:
        print(f"[-] An error occured during the protection of the app {apk_path}. Error : {str(e)}")
        pass

    print(f"[*] Protecting app {apk_path} finish", flush=True)

if __name__ == "__main__":
    args = parse_args()

    apks = get_apk_list(args.apk_folder)
    lock = Lock()
    
    # from DbManager import db_manager as db
    db_manager = DBManager(create_engine(args.host))

    perms = list() 
    for ps in args.permutations.split(';'):
        p1, p2, p3 = ps.split(',')
        perms.append((int(p1), int(p2), int(p3)))

    for p1, p2, p3 in perms:
        print(f"[*] Pool of {args.processes} processes with parameter chance_extractor={p1}, chance_encryptor={p2}, chance_injector={p3} is starting")
        p = Pool(processes=int(args.processes))
        params = [(apk, p1, p2, p3, args.output_folder, args.keep_output_files) for apk in apks]
        p.starmap(protect_apk, params)
        p.close()
        print(f"[*] Pool with parameter chance_extractor={p1}, chance_encryptor={p2}, chance_injector={p3} ended", flush=True)
    
    
