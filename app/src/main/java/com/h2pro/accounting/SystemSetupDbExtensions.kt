package com.h2pro.accounting

import android.content.ContentValues

/** عمليات التعديل والحذف والبحث الخاصة بتهيئة النظام، على نفس قاعدة h2pro.db. */
fun AccountingDb.updateYear(oldYear:Int,newYear:Int,months:String,status:String):Boolean = writableDatabase.update("financial_years",ContentValues().apply{put("year",newYear);put("months",months);put("status",status)},"year=?",arrayOf(oldYear.toString()))>0
fun AccountingDb.deleteYear(year:Int):Boolean = writableDatabase.delete("financial_years","year=?",arrayOf(year.toString()))>0
fun AccountingDb.searchYears(q:String):String = readableDatabase.rawQuery("SELECT year||' | '||months||' | '||status FROM financial_years WHERE CAST(year AS TEXT) LIKE ? OR months LIKE ? ORDER BY year DESC",arrayOf("%$q%","%$q%")).use{c->val a=mutableListOf<String>();while(c.moveToNext())a.add(c.getString(0));a.joinToString("\n")}
fun AccountingDb.deleteCompany():Boolean = writableDatabase.update("company",ContentValues().apply{put("name","");put("phone","");put("address","");put("logo","")},"id=1",null)>0
fun AccountingDb.updateRegion(oldCountry:String,oldProvince:String,oldCity:String,oldDistrict:String,country:String,province:String,city:String,district:String):Boolean = writableDatabase.update("regions",ContentValues().apply{put("country",country);put("province",province);put("city",city);put("district",district)},"country=? AND province=? AND city=? AND district=?",arrayOf(oldCountry,oldProvince,oldCity,oldDistrict))>0
fun AccountingDb.deleteRegion(country:String,province:String,city:String,district:String):Boolean = writableDatabase.delete("regions","country=? AND province=? AND city=? AND district=?",arrayOf(country,province,city,district))>0
fun AccountingDb.searchRegions(country:String,province:String,city:String,district:String):String = readableDatabase.rawQuery("SELECT country||' | '||province||' | '||city||' | '||district FROM regions WHERE country LIKE ? AND province LIKE ? AND city LIKE ? AND district LIKE ? ORDER BY country,province,city",arrayOf("%$country%","%$province%","%$city%","%$district%")).use{c->val a=mutableListOf<String>();while(c.moveToNext())a.add(c.getString(0));a.joinToString("\n")}
fun AccountingDb.updateCurrency(name:String,equivalent:Double,local:Boolean,rate:Double):Boolean = writableDatabase.update("currencies",ContentValues().apply{put("equivalent",equivalent);put("is_local",if(local)1 else 0);put("exchange_rate",rate)},"name=?",arrayOf(name))>0
fun AccountingDb.deleteCurrency(name:String):Boolean = writableDatabase.delete("currencies","name=?",arrayOf(name))>0
fun AccountingDb.searchCurrencies(q:String):String = readableDatabase.rawQuery("SELECT name||' | معادل: '||equivalent||' | '||CASE WHEN is_local=1 THEN 'محلية' ELSE 'أجنبية' END||' | تحويل: '||exchange_rate FROM currencies WHERE name LIKE ? ORDER BY name",arrayOf("%$q%")).use{c->val a=mutableListOf<String>();while(c.moveToNext())a.add(c.getString(0));a.joinToString("\n")}
