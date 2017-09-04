package com.bing.diginius.daoImpl;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.bing.diginius.dao.SaveToMySqlDao;

@Service
public class SaveToMySqlDaoImpl implements SaveToMySqlDao {

	@Autowired
	DataSource dataSource;

	@Override
	public void saveAllInfoToMySql(Long accountId, Long customer_id, String CustomerName, String CustomerBingId,
			Long primaryUserId, String refrehToken) {

		try {
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			String sqlQuery = "Insert into microsoft_bing_client_config (account_id,customer_id,customer_name,"
					+ "bing_id,bing_primary_id, refresh_token)" + " Values (" + accountId + "," + customer_id 
					+ ",'" + CustomerName + "','" + CustomerBingId + "'," + primaryUserId + ",'" + refrehToken + "')";

			jdbcTemplate.execute(sqlQuery);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
