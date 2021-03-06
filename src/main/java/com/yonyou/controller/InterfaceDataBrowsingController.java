package com.yonyou.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eova.common.base.BaseController;
import com.eova.common.utils.xx;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;
import com.jfinal.plugin.activerecord.Record;
import com.yonyou.util.UUID;

/**
 * 
 * @author tanglibing
 *	接口数据浏览
 */
public class InterfaceDataBrowsingController extends BaseController{
	
	String[] primaryKey;
	String[] smetas;//=new String[] {"query","input","output","mobile"};
	Record re;
	String metadata_id;
	
	public InterfaceDataBrowsingController() {}
	
	public InterfaceDataBrowsingController(String metadata_id,String[] smetas) {
		this.metadata_id=metadata_id;
		this.smetas=smetas;
	}
	
	/*
	 * 页面跳转
	 */
	public void forwordHtml() {
		render("/eova/interfaceDataBrowsing/interfaceDataBrowsing.html");
	}
	
	/*
	 * 查询接口数据类别
	 */
	public void queryClass() {
		List<Record> record=Db.use(xx.DS_EOVA).find("select id,sy_regeist_name from bs_system_registry");
		renderJson(record);
	}
	/*
	 * 接口数据表
	 */
	public void queryClassTable() {
		String classId=getPara(0);
		List<Record> list=Db.use(xx.DS_EOVA).find("select id,table_code,table_name from bs_metadata where syregeist_id='"+classId+"'");
		renderJson(list);
	}
	/*
	 * 初始化模板
	 */
	public void initTemplate() {
		String metadata_id=getPara(0);
		String[] type=new String[] {"query","input","output","mobile"};
		renderJson(new InterfaceDataBrowsingController(metadata_id,type).queryTemplateIsInit("数据浏览"));
	}
	/*
	 * 查询模板是否已被初始化
	 */
	public String queryTemplateIsInit(String smeta) {
		
		String bs_metadata_is="select id,table_code,table_name from bs_metadata where id='"+metadata_id+"'";
		Record queryData=Db.use(xx.DS_EOVA).findFirst(bs_metadata_is);
		
		if(queryData==null) 
		return "{\"message\":\"接口表结构可能已被删除\"}";
		
		String bs_style_is="select count(1) from bs_style"
				+ " where sid='"+queryData.getStr("id")+"' and "
				+ " md_table='"+queryData.getStr("table_code")+"' and "
				+ " smeta='"+smeta+"'";
		
		int isTrue=Db.use(xx.DS_EOVA).queryInt(bs_style_is);
		if(isTrue>0)
		return "{\"message\":\"接口模板已被初始化\"}";

		re=new Record();
		re.set("dr", 0);
		re.set("sid", queryData.getStr("id"));
		re.set("md_table", queryData.getStr("table_code"));
		re.set("smeta", smeta);
		
		//控制事务提交
		boolean temp=Db.use(xx.DS_EOVA).tx(new IAtom() {
			@Override
			public boolean run() throws SQLException {
				// TODO Auto-generated method stub
				boolean isStyle=save_bs_tyle();
				
				if(!isStyle)
				return isStyle;
				
				String bs_metadata_b="select * from bs_metadata_b where pid='"+metadata_id+"'";
				List<Record> fields=Db.use(xx.DS_EOVA).find(bs_metadata_b);
				if(fields.size()<1)
				return false;
				
				isStyle=save_bs_style_b(fields);
				
				return isStyle;
			}
		});
		
		if(!temp)
		return "{\"message\":\"接口模板数据校验失败\"}";
		
		return "{\"message\":\"接口模板初始化成功\"}";
	}
	/*
	 * 模板初始化
	 */
	public boolean save_bs_tyle() {
		primaryKey=new String[4];
		boolean isStyle=false;
		int pri_id=0;
		for(String sm:smetas) {
			primaryKey[pri_id]=UUID.getUnqionPk();
			re.set("id", primaryKey[pri_id]);
			re.set("type", sm);
			isStyle= Db.use(xx.DS_EOVA).save("bs_style", re);
			pri_id++;
		}
		return isStyle;
	}
	/*
	 * 模板初始化表体
	 */
	public boolean save_bs_style_b(List<Record> fields) {
		Record initFields=new Record();
		boolean isTrue=false;
		for(int i=0;i<primaryKey.length;i++) {
			initFields.set("pid", primaryKey[i]);
			for(int j=0;j<fields.size();j++) {
				Record re_fields=fields.get(j);
				initFields.set("id", UUID.getUnqionPk());
				initFields.set("col_code", re_fields.getStr("field_code"));
				initFields.set("col_name", re_fields.getStr("field_name"));
				initFields.set("col_length", re_fields.getStr("field_length"));
				initFields.set("col_align", "center");
				initFields.set("col_decimal", 0);
				initFields.set("dr", 0);
				initFields.set("isshow", 0);
				initFields.set("isedit", 0);
				initFields.set("input_type", "text");
				initFields.set("metadataid", re_fields.getStr("id"));
				isTrue=Db.use(xx.DS_EOVA).save("bs_style_b", initFields);
			}
		}
		return isTrue;
	}
	/*
	 * 接口列查询
	 */
	public void queryInterfaceFields() {
		String tableId=getPara(0);
		String sql_id="select id from bs_style where sid='"+tableId+"'";
		
		String sql_pid=Db.use(xx.DS_EOVA).queryStr(sql_id);
		
		if(sql_pid==null) {
			renderJson("");
			return;
		}
		
		String sql_fields="select * from bs_style_b where pid='"+sql_pid+"' and isshow=0";
		List<Record> re=Db.use(xx.DS_EOVA).find(sql_fields);
		
		if(re.size()>0)
		renderJson(re);
		else
		renderJson("{\"message\":\"无数据列\"}");
	}
	/*
	 * 查询接口表体数据
	 */
	public void queryInterface() {
		
		String objectCode=getPara(0);
		String objectId=getPara(1);
		
		String style_sql="select sid from bs_style where sid='"+objectId+"' and md_table='"+objectCode+"' and smeta='数据浏览'";
		String isExist=Db.use(xx.DS_EOVA).queryStr(style_sql);
		if(isExist==null||"".equals(isExist)) {
			renderJson("");
			return;
		}
		
		String sql="select * from "+objectCode;
		List<Record> re=Db.use(xx.DS_MAIN).find(sql);
		renderJson(re);
	}
	/*
	 * 界面right datagrid条件查询
	 */
	public void queryWhere() {
		String tableCode=getPara("tableCode");
		String[] jsonArr=getParaValues("arr");
		JSONArray jsonArrary=JSONArray.parseArray(jsonArr[0].toString());
		StringBuffer sb=new StringBuffer();
		sb.append("select ");
		String fields="";
		for(int i=0;i<jsonArrary.size();i++) {
			JSONObject jsonObj=(JSONObject)jsonArrary.get(i);
			if(jsonObj.getString("field").length()<1||" ".equals(jsonObj.getString("field"))) {
				continue;
			}
			fields+=jsonObj.getString("field").substring(1,jsonObj.getString("field").length())+",";
			
		}
		sb.append(fields.substring(0,fields.lastIndexOf(",")));
		
		String table_sql="select table_code from bs_metadata where id='"+tableCode+"'";
		String table_code=Db.use(xx.DS_EOVA).queryStr(table_sql);
		sb.append(" from "+table_code);
		
		boolean isWhereBrackets=false;
		boolean isWhereField=false;
		boolean isWhereExpression=false;
		boolean isWhereValue=false;
		boolean isWhereAndOr=false;
		for(int i=0;i<jsonArrary.size();i++) {
			JSONObject jsonObj=(JSONObject)jsonArrary.get(i);
			String brackets=jsonObj.getString("brackets");
			String field=jsonObj.getString("field");
			String expression=jsonObj.getString("expression");
			String value=jsonObj.getString("value");
			String andOr=jsonObj.getString("andOr");
			if(brackets.length()<1||brackets==null) {
				isWhereBrackets=true;
			}else {
				isWhereBrackets=false;
			}
			if(field.length()<1||field==null) {
				isWhereField=true;
			}else {
				isWhereField=false;
			}
			if(expression.length()<1||expression==null) {
				isWhereExpression=true;
			}else {
				isWhereExpression=false;
			}
			if(value.length()<1||value==null) {
				isWhereValue=true;
			}else {
				isWhereValue=false;
			}
			if(andOr.length()<1||andOr==null) {
				isWhereAndOr=true;
			}else {
				isWhereAndOr=false;
			}
		}
		try {
			if (isWhereField && isWhereBrackets && isWhereExpression && isWhereAndOr) {
				if (!isWhereValue) {
					List<Record> talbe = Db.use(xx.DS_MAIN).find(sb.toString());
					renderJson(talbe);
				} else {
					sb.append(" where ");
					for (int i = 0; i < jsonArrary.size(); i++) {
						JSONObject jsonObj = (JSONObject) jsonArrary.get(i);
						String brackets = jsonObj.getString("brackets");
						String field = jsonObj.getString("field").substring(1, jsonObj.getString("field").length());
						String expression = jsonObj.getString("expression");
						String value = jsonObj.getString("value");
						String andOr = jsonObj.getString("andOr");
						if(" ".equals(value)) {
							sb.append(brackets + " " + field + " " + expression + " " + value + " " + andOr);
						}else {
							sb.append(brackets + " " + field + " " + expression + " '" + value + "' " + andOr);
						}
					}
					List<Record> appendSql = Db.use(xx.DS_MAIN).find(sb.toString());
					renderJson(appendSql);
				}
			} else {
				sb.append(" where ");
				for (int i = 0; i < jsonArrary.size(); i++) {
					JSONObject jsonObj = (JSONObject) jsonArrary.get(i);
					String brackets = jsonObj.getString("brackets");
					String field = jsonObj.getString("field").substring(1, jsonObj.getString("field").length());
					String expression = jsonObj.getString("expression");
					String value = jsonObj.getString("value");
					String andOr = jsonObj.getString("andOr");
					if(" ".equals(value)) {
						sb.append(brackets + " " + field + " " + expression + " " + value + " " + andOr);
					}else {
						sb.append(brackets + " " + field + " " + expression + " '" + value + "' " + andOr);
					}
				}
				List<Record> appendSql = Db.use(xx.DS_MAIN).find(sb.toString());
				renderJson(appendSql);
			} 
		} catch (Exception e) {
			// TODO: handle exception
			renderJson(e.getMessage());
		}
	}
	/*
	 * 界面常量
	 */
	public void expression() {
		renderJson("["
				+ "{\"expression_id\":\">\",\"expression_name\":\"大于\"},"
				+ "{\"expression_id\":\"<\",\"expression_name\":\"小于\"},"
				+ "{\"expression_id\":\"=\",\"expression_name\":\"等与\"},"
				+ "{\"expression_id\":\"!=\",\"expression_name\":\"不等于\"},"
				+ "{\"expression_id\":\"in\",\"expression_name\":\"包含\"},"
				+ "{\"expression_id\":\"not in\",\"expression_name\":\"不包含\"}"
				+ "]");
	}
	public void brackets() {
		renderJson("["
				+ "{\"bracketsVal\":\"(\",\"bracketsText\":\"(\"},"
				+ "{\"bracketsVal\":\")\",\"bracketsText\":\")\"}"
				+ "]");
	}
	public void andOr() {
		renderJson("["
				+ "{\"andOrVal\":\"and\",\"andOrText\":\"and\"},"
				+ "{\"andOrVal\":\"or\",\"andOrText\":\"or\"}"
				+ "]");
	}
}
