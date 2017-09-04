package com.bing.diginius.dao;

public interface SaveToMySqlDao {

	public void saveAllInfoToMySql(Long accountId, Long customer_id, String CustomerName, String CustomerBingId,
			Long primaryUserId, String refrehToken);

}
