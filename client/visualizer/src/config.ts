/**
 * All of the top-level tunable options for the game client.
 */
export interface Config {
  /**
   * The version of the game we're simulating.
   */
  readonly gameVersion: string;

  /**
   * Whether to try to run the game in full-screen
   */
  readonly fullscreen: boolean;

  /**
   * Dimensions of the canvas
   */
  readonly width: number;
  readonly height: number;

  /**
   * Controls resolution of the canvas.
   */
  readonly upscale: number;

  /**
   * Turns per second.
   *
   * (DISTINCT from fps!)
   */
  readonly defaultTPS: number; // TODO: use this.

  /**
   * The url to listen for websocket data on, if any.
   */
  readonly websocketURL: string | null;

  /**
   * The match file URL to load when we start.
   */
  readonly matchFileURL: string | null;

  /**
   * How often to poll the server via websocket, in ms.
   */
  readonly pollEvery: number;

  /**
   * Whether tournament mode is enabled.
   */
  readonly tournamentMode: boolean;

  /**
   * Whether or not to interpolate between frames.
   */
  interpolate: boolean;

  /**
   * Whether or not to display indicator dots and lines
   */
  indicators: boolean;

  /**
   * Whether or not to display the action radius.
   */
  seeActionRadius: boolean;

  /**
   * Whether or not to display the sensor radius.
   */
  seeSensorRadius: boolean;

  /**
   * Whether or not to display the detection radius.
   */
  seeDetectionRadius: boolean;

  /**
   * The mode of the game
   */
  mode: Mode;

  /**
   * Whether to display the splash screen.
   */
  splash: boolean;

  /**
   * Whether to display the grid
   */
  showGrid: boolean;

  /**
   * Viewoption for Swamp
   */
  viewSwamp: boolean;

  /**
   * Whether logs should show shorter header
   */
  shorterLogHeader: boolean;

  /**
   * Whether we should process a match's logs by default.
   */
  processLogs: boolean;
}

/**
 * Different game modes that determine what is displayed on the client
 */
export enum Mode {
  GAME,
  HELP,
  LOGS,
  RUNNER,
  QUEUE,
  MAPEDITOR,
  PROFILER
}

/**
 * Handle setting up any values that the user doesn't set.
 */
export function defaults(supplied?: any): Config {
  let conf: Config = {
    gameVersion: "2021.2.3.0", //TODO: Change this on each release!
    fullscreen: false,
    width: 600,
    height: 600,
    upscale: 1800,
    defaultTPS: 20,
    websocketURL: null,
    matchFileURL: null,
    pollEvery: 500,
    tournamentMode: false,
    interpolate: true,
    indicators: false,
    mode: Mode.QUEUE,
    splash: true,
    seeActionRadius: false,
    seeSensorRadius: false,
    seeDetectionRadius: false,
    showGrid: false,
    viewSwamp: true,
    shorterLogHeader: false,
    processLogs: true
  };
  return Object.assign(conf, supplied);
}
