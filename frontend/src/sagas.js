import { fork, call } from 'redux-saga/effects'

import createWsConnection from './utils/createWebSocketConnection'

import errorSaga from './containers/App/saga'
import homeSaga from './containers/HomePage/saga'

function* wsAwareSagas() {
  let wsConnection
  try {
    wsConnection = yield call(createWsConnection)
  } catch (error) {
    console.error('Could not connect to socket')
    return
  }

  yield fork(errorSaga, wsConnection)
  yield fork(homeSaga, wsConnection)
}

export default function* rootSaga() {
  yield [
    call(wsAwareSagas)
  ]
}