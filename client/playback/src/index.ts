import GameWorld from './gameworld';
import * as gameworld from './gameworld';
import Metadata from './metadata';
import * as metadata from './metadata';
import StructOfArrays from './soa';
import * as soa from './soa';
import Match from './match';
import { Log } from './gameworld';
import Game, {playbackConfig} from './game';
import { flatbuffers, schema } from 'battlecode-schema';

export {Game, Log, Match, GameWorld, gameworld, Metadata, metadata, StructOfArrays, soa, flatbuffers, schema, playbackConfig};

// TODO provide ergonomic main export
