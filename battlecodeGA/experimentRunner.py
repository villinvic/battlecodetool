""" 
Author : goji .
Date : 14/01/2021 .
File : experimentRunner.py .

Description : None

Observations : None
"""

# == Imports ==
from battlecodeGA.gameRunner import GameRunner
from os import listdir
from os.path import isfile, join
from pprint import pprint
import signal
# =============

def RUN(map_path="engine/src/main/battlecode/world/resources/", bot_path="example-bots/src/main/", tested="FullChocoV1",
        n_workers=4, windows=False):

    log_path = tested + ".log"
    print("[EXPERIMENT]\nrunning with:")
    pprint(locals())

    maps = [f.split(".map21")[0] for f in listdir(map_path) if isfile(join(map_path, f))]
    bots = [f for f in listdir(bot_path) if (not isfile(join(bot_path, f)) and f!=tested)]
    matches = {}
    dummy_match = GameRunner("", "", "")
    procs = n_workers * [dummy_match]

    for i, bot in enumerate(bots):
        for j, map in enumerate(maps):
            matches[i*len(maps)+j] = GameRunner(map, tested, bot, windows)

    wins = 0
    last_index = 1
    print("Games to run:")
    pprint(matches)
    print("[STARTING COMPARISON...]")
    try:
        for i in range(len(bots)*len(maps)):
            res = procs[i % n_workers].join()
            # None if no match started
            if res == 0:
                wins += 1
            if i >= n_workers:
                last_index = i + 1 - n_workers
                print("[Progress] %d/%d (%.3f)" % (wins, 1+i-n_workers - wins, wins / float(1+i-n_workers)))
            procs[i % n_workers] = matches[i]
            print("Match %d/%d running..." % (1+i, len(bots)*len(maps)))
            procs[i % n_workers].run_game()

        for proc in procs:
            res = proc.join()
            if res == 0:
                wins += 1
    except KeyboardInterrupt:
        for proc in procs:
            res = proc.game.terminate()
        print('Interrupted experiment !')
    print("Done.\nPrinting results :")
    with open(log_path, "w") as output_file:
        for i in matches.keys():
            result = "[%d] %s" %(i, matches[i].result)
            output_file.write(result+"\n")
            print(result)

        results = "[FINAL RESULTS]\n%d/%d (%.2f%%)" % (wins, last_index-wins, 100 * wins / float(last_index))
        output_file.write(results)
        print(results)








