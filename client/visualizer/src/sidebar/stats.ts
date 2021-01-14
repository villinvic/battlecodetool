import {Config} from '../config';
import * as cst from '../constants';
import {AllImages} from '../imageloader';
import {schema} from 'battlecode-playback';
import Runner from '../runner';

const hex: Object = {
  1: "#db3627",
  2: "#4f7ee6"
};

export type VoteBar = {
  bar: HTMLDivElement,
  label: HTMLSpanElement
};

/**
* Loads game stats: team name, votes, robot count
* We make the distinction between:
*    1) Team names - a global string identifier i.e. "Teh Devs"
*    2) Team IDs - each Battlecode team has a unique numeric team ID i.e. 0
*    3) In-game ID - used to distinguish teams in the current match only;
*       team 1 is red, team 2 is blue
*/
export default class Stats {

  readonly div: HTMLDivElement;
  private readonly images: AllImages;

  private readonly tourIndexJump: HTMLInputElement;

  // Key is the team ID
  private robotTds: Object = {}; // Secondary key is robot type

  private voteBars: VoteBar[];
  private voteBarsElement: HTMLTableElement;
  private maxVotes: number;

  private relativeBars: HTMLDivElement[];
  private relativeBarsElement: HTMLElement;

  private robotConsole: HTMLDivElement;

  private runner: Runner; //needed for file uploading in tournament mode

  private conf: Config;

  // Note: robot types and number of teams are currently fixed regardless of
  // match info. Keep in mind if we ever change these, or implement this less
  // statically.

  readonly robots: schema.BodyType[] = cst.bodyTypeList;

  constructor(conf: Config, images: AllImages, runner: Runner) {
    this.conf = conf;
    this.images = images;
    this.div = document.createElement("div");
    this.tourIndexJump = document.createElement("input");
    this.runner = runner;

    let teamNames: Array<string> = ["?????", "?????"];
    let teamIDs: Array<number> = [1, 2];
    this.voteBarsElement = document.createElement("table");
    this.initializeGame(teamNames, teamIDs);
  }

  /**
   * Colored banner labeled with the given teamName
   */
  private teamHeaderNode(teamName: string, inGameID: number) {
    let teamHeader: HTMLDivElement = document.createElement("div");
    teamHeader.className += ' teamHeader';

    let teamNameNode = document.createTextNode(teamName);
    teamHeader.style.backgroundColor = hex[inGameID];
    teamHeader.appendChild(teamNameNode);
    return teamHeader;
  }

  /**
   * Create the table that displays the robot images along with their counts.
   * Uses the teamID to decide which color image to display.
   */
  private robotTable(teamID: number, inGameID: number): HTMLTableElement {
    let table: HTMLTableElement = document.createElement("table");
    table.setAttribute("align", "center");

    // Create the table row with the robot images
    let robotImages: HTMLTableRowElement = document.createElement("tr");
    
    // Create the table row with the robot counts
    let robotCounts: HTMLTableRowElement = document.createElement("tr");

    for (let robot of this.robots) {
      let robotName: string = cst.bodyTypeToString(robot);
      let tdRobot: HTMLTableCellElement = document.createElement("td");
      tdRobot.className = "robotSpriteStats";

      const img = this.images.robots[robotName][inGameID];
      img.style.width = "64px";
      img.style.height = "64px";

      tdRobot.appendChild(img);

      robotImages.appendChild(tdRobot);

      let tdCount: HTMLTableCellElement = this.robotTds[teamID][robot];
      robotCounts.appendChild(tdCount);
    }
    table.appendChild(robotImages);
    table.appendChild(robotCounts);

    return table;
  }

  private initVoteBars(teamIDs: Array<number>) {
    const voteBars: VoteBar[] = [];
    teamIDs.forEach((teamID: number) => {
      let votes = document.createElement("div");
      votes.className = "stat-bar";
      votes.style.backgroundColor = hex[teamID];
      let votesSpan = document.createElement("span");
      votesSpan.innerHTML = "0";
      // Store the stat bars
      voteBars[teamID] = {
          bar: votes,
          label: votesSpan
      };
    });
    return voteBars;
  }

  private getVoteBarElement(teamIDs: Array<number>): HTMLTableElement {
    const table = document.createElement("table");
    const bars = document.createElement("tr");
    const counts = document.createElement("tr");
    table.id = "stats-table";
    bars.id = "stats-bars";
    table.setAttribute("align", "center");

    const title = document.createElement('td');
    title.colSpan= 2;
    const label = document.createElement('h3');
    label.innerText = 'Votes';

    teamIDs.forEach((id: number) => {
      const bar = document.createElement("td");
      bar.height = "150";
      bar.vAlign = "bottom";
      bar.appendChild(this.voteBars[id].bar);
      bars.appendChild(bar);

      const count = document.createElement("td");
      count.appendChild(this.voteBars[id].label);
      counts.appendChild(count);
    });

    title.appendChild(label);
    table.appendChild(title);
    table.appendChild(bars);
    table.appendChild(counts);
    return table;
  }

  private initRelativeBars(teamIDs: Array<number>) {
    const relativeBars: HTMLDivElement[] = [];
    teamIDs.forEach((id: number) => {
      const bar = document.createElement("div");
      bar.style.backgroundColor = hex[id];
      bar.style.width = `50%`;
      bar.className = "influence-bar";
      bar.innerText = "0";

      relativeBars[id] = bar;
    });
    return relativeBars;
  }

  private getRelativeBarsElement(teamIDs: Array<number>): HTMLElement {
    const div = document.createElement("div");
    div.setAttribute("align", "center");

    const title = document.createElement('td');
    title.colSpan= 2;
    const label = document.createElement('h3');
    label.innerText = 'Influence stored in ECs';
    
    const frame = document.createElement("div");
    frame.style.width = "250px";
    frame.style.height = "30px";
    
    teamIDs.forEach((id: number) => {
      frame.appendChild(this.relativeBars[id]);
    });

    div.appendChild(label);
    div.appendChild(frame);
    return div;
  }

  /**
   * Clear the current stats bar and reinitialize it with the given teams.
   */
  initializeGame(teamNames: Array<string>, teamIDs: Array<number>){
    // Remove the previous match info
    while (this.div.firstChild) {
      this.div.removeChild(this.div.firstChild);
    }
    this.robotTds = {};
    this.voteBars = [];
    this.relativeBars = [];
    this.maxVotes = 1500;

    if(this.conf.tournamentMode){
      // FOR TOURNAMENT
      let uploadButton = this.runner.getUploadButton();
      let tempdiv = document.createElement("div");
      tempdiv.className = "upload-button-div";
      tempdiv.appendChild(uploadButton);
      this.div.appendChild(tempdiv);

      // add text input field
      this.tourIndexJump.type = "text";
      this.tourIndexJump.onkeyup = (e) => { this.tourIndexJumpFun(e) };
      this.tourIndexJump.onchange = (e) => { this.tourIndexJumpFun(e) };
      this.div.appendChild(this.tourIndexJump);
    }
    
    // Populate with new info
    // Add a section to the stats bar for each team in the match
    for (var index = 0; index < teamIDs.length; index++) {
      // Collect identifying information
      let teamID = teamIDs[index];
      let teamName = teamNames[index];
      let inGameID = index + 1; // teams start at index 1

      // A div element containing all stats information about this team
      let teamDiv = document.createElement("div");

      // Create td elements for the robot counts and store them in robotTds
      // so we can update these robot counts later; maps robot type to count
      let initialRobotCount: Object = {};
      for (let robot of this.robots) {
        let td: HTMLTableCellElement = document.createElement("td");
        td.innerHTML = "0";
        initialRobotCount[robot] = td;
      }
      this.robotTds[teamID] = initialRobotCount;
      // Add the team name banner and the robot count table
      teamDiv.appendChild(this.teamHeaderNode(teamName, inGameID));
      teamDiv.appendChild(this.robotTable(teamID, inGameID));
      teamDiv.appendChild(document.createElement("br"));

      this.div.appendChild(teamDiv);
    }

    this.div.appendChild(document.createElement("hr"));

    // TODO relative bar
    // this.div.appendChild(this.relativeBarElement);
    // console.log(this.relativeBarElement)
    
    // Add stats table
    this.voteBarsElement.remove();
    this.voteBars = this.initVoteBars(teamIDs);
    this.voteBarsElement = this.getVoteBarElement(teamIDs);
    this.div.appendChild(this.voteBarsElement);

    this.relativeBars = this.initRelativeBars(teamIDs);
    this.relativeBarsElement = this.getRelativeBarsElement(teamIDs);
    this.div.appendChild(this.relativeBarsElement);
  }

  tourIndexJumpFun(e) {
    if (e.keyCode === 13){
        var h = +this.tourIndexJump.value.trim().toLowerCase();
        this.runner.seekTournament(h-1);
    }
  }

  /**
   * Change the robot count on the stats bar
   */
  setRobotCount(teamID: number, robotType: schema.BodyType, count: number) {
    let td: HTMLTableCellElement = this.robotTds[teamID][robotType];
    td.innerHTML = String(count);
  }

  /**
   * Change the votes of the given team
   */
  setVotes(teamID: number, count: number) {
    // TODO: figure out if statbars.get(id) can actually be null??
    const statBar: VoteBar = this.voteBars[teamID];
    statBar.label.innerText = String(count);
    this.maxVotes = Math.max(this.maxVotes, count);
    statBar.bar.style.height =`${Math.min(100 * count / this.maxVotes, 100)}%`;

    // TODO add reactions to relative bars
    // TODO get total votes to get ratio
    // this.relBars[teamID].width;

    // TODO winner gets star?
    // if (this.images.star.parentNode === statBar.bar) {
    //   this.images.star.remove();
    // }
  }

  setInfluence(teamID: number, influence: number, totalInfluence: number) {
    const relBar: HTMLDivElement = this.relativeBars[teamID];
    relBar.innerText = String(influence);
    if (totalInfluence == 0) relBar.style.width = '50%';
    else relBar.style.width =  String(Math.round(influence * 100 / totalInfluence)) + "%";
  }
}
