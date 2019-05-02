// @flow
import { Button, Classes } from '@blueprintjs/core'
import type { List } from 'immutable';
import React from 'react';
import { connect } from 'react-redux';
import type { ApiLobby } from '../../api/model';
import type { GlobalState } from '../../reducers';
import { actions } from '../../redux/actions/lobby';
import { getAllGames } from '../../redux/games';
import './GameList.css';
import { GameStatus } from './GameStatus';
import { PlayerCount } from './PlayerCount';

type GameListProps = {
  games: List<ApiLobby>,
  joinGame: (gameId: string) => void,
};

const GameListPresenter = ({ games, joinGame }: GameListProps) => (
        <table className={Classes.HTML_TABLE}>
          <thead>
          <GameListHeaderRow />
          </thead>
          <tbody>
          {games.map((game: ApiLobby) => <GameListItemRow key={game.id} game={game} joinGame={joinGame}/>)}
          </tbody>
        </table>
);

const GameListHeaderRow = () => (
  <tr>
    <th>Name</th>
    <th>Status</th>
    <th>Nb Players</th>
    <th>Join</th>
  </tr>
);

const GameListItemRow = ({game, joinGame}) => (
  <tr className="gameListRow">
    <td>{game.name}</td>
    <td>
      <GameStatus state={game.state} />
    </td>
    <td>
      <PlayerCount nbPlayers={game.players.length} />
    </td>
    <td>
      <JoinButton game={game} joinGame={joinGame}/>
    </td>
  </tr>
);

const JoinButton = ({game, joinGame}) => {
  const disabled = game.state !== 'LOBBY';
  const onClick = () => joinGame(game.id);
  return <Button minimal disabled={disabled} icon='arrow-right' title='Join Game' onClick={onClick}/>;
};

const mapStateToProps = (state: GlobalState) => ({
  games: getAllGames(state),
});

const mapDispatchToProps = {
  joinGame: actions.requestJoinGame,
};

export const GameList = connect(mapStateToProps, mapDispatchToProps)(GameListPresenter);

