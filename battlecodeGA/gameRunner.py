
"""
Author : goji .
Date : 11/01/2021 .
File : individual.py .

Description : None

Observations : None
"""

# == Imports ==
import subprocess
# =============


class GameRunner:

    def __init__(self, map, p1, p2, windows=False):
        self.windows = windows
        self.base_cmd = "gradlew headless" if windows else "./gradlew headless"
        self.game = None
        self.map = map
        self.players = (p1, p2)

        self.result = "NO RESULT"

    def __repr__(self):
        return "Game(Map=%s, p1=%s, p2=%s)" % (self.map, *self.players)

    def build_cmd(self):
        cmd = self.base_cmd + " -Pmaps=%s -PteamA=%s -PteamB=%s" % (self.map, *self.players)
        if self.windows:
            cmd += " > NUL"
        return cmd

    def run_game(self):
        if self.windows:
            self.game = subprocess.Popen(self.build_cmd().split(), shell=False)
        else:
            self.game = subprocess.Popen(self.build_cmd().split(), shell=False, stdout=subprocess.DEVNULL,
                                     stderr=subprocess.DEVNULL, stdin=subprocess.DEVNULL)

    def join(self):
        if self.game is None:
            return None

        winner = self.game.wait()
        loser = winner - 1
        if winner == 0:
            self.result = "Team %s WON vs %s ON %s" % (*self.players, self.map)
        else:
            self.result = "Team %s LOST vs %s ON %s" % (*self.players, self.map)
        return winner
