import { request, toQuery } from './request';

export const statisticsApi = {
  dashboard(params) {
    return request(`/api/v1/statistics/dashboard${toQuery(params)}`);
  }
};
