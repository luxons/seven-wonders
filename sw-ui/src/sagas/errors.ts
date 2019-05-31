import {Toaster} from '@blueprintjs/core';
import {Channel, eventChannel} from 'redux-saga';
import {apply, cancelled, take} from 'redux-saga/effects';
import {ApiError} from '../api/model';
import {SevenWondersSession} from '../api/sevenWondersApi';

const ErrorToaster = Toaster.create();

export function* errorHandlingSaga(session: SevenWondersSession): any {
  const errorChannel: Channel<ApiError> = yield eventChannel(session.watchErrors());
  try {
    while (true) {
      const error: ApiError = yield take(errorChannel);
      yield* handleOneError(error);
    }
  } finally {
    if (yield cancelled()) {
      console.log('Error management saga cancelled');
      yield apply(errorChannel, errorChannel.close);
    }
  }
}

function* handleOneError(err: ApiError): any {
  console.error('Error received on web socket channel', err);
  const msg = buildMsg(err);
  yield apply(ErrorToaster, ErrorToaster.show, [{ intent: 'danger', icon: 'error', message: msg }]);
}

function buildMsg(err: ApiError): string {
  if (err.details.length > 0) {
    return err.details.map(d => d.message).join('\n');
  } else {
    return err.message;
  }
}