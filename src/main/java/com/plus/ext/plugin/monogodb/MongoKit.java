/**
 * Copyright (c) 2009-2016, LarryKoo 老古 (gumutianqi@gmail.com)
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.plus.ext.plugin.monogodb;

import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.mongodb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

public class MongoKit {

    protected static Log logger = Log.getLog(MongoKit.class);

    private static MongoClient client;
    private static DB defaultDb;

    public static void init(MongoClient client, String database) {
        MongoKit.client = client;
        MongoKit.defaultDb = client.getDB(database);
    }

    public static void updateFirst(String collectionName, Map<String, Object> src, Map<String, Object> dist) {
        MongoKit.getCollection(collectionName).findAndModify(toDBObject(src), toDBObject(dist));
    }

    public static Boolean drop(String collectionName) {
        Boolean bool = true;
        try {
            MongoKit.getCollection(collectionName).drop();
        } catch (Exception e) {
            bool = false;
        } finally {
            return bool;
        }
    }

    public static int removeAll(String collectionName) {
        return MongoKit.getCollection(collectionName).remove(new BasicDBObject()).getN();
    }

    public static int remove(String collectionName, Map<String, Object> filter) {
        return MongoKit.getCollection(collectionName).remove(toDBObject(filter)).getN();
    }

    public static int remove(String collectionName, BasicDBObject dbObject) {
        return MongoKit.getCollection(collectionName).remove(toDBObject(dbObject)).getN();
    }

    /**
     * 封装好的索引创建接口
     * MongoKit.createIndex("xxx", "_pcode", true);
     *
     * @param collectionName
     * @param keys
     * @param unique
     */
    public static void createIndex(String collectionName, String keys, Boolean unique) {
        if (unique) {
            createIndex(collectionName, new BasicDBObject(keys, 1L), new BasicDBObject("unique", true));
        } else {
            createIndex(collectionName, new BasicDBObject(keys, 1L), null);
        }
    }

    /**
     * MongoKit.createIndex("", new BasicDBObject("_pcode", 1L), new BasicDBObject("unique", true));
     * MongoKit.createIndex("", new BasicDBObject("_pcode", 1L));
     *
     * @param collectionName
     * @param keys
     */
    public static void createIndex(String collectionName, BasicDBObject keys, BasicDBObject option) {
        if (null != option) {
            MongoKit.getCollection(collectionName).createIndex(keys, option);
        } else {
            MongoKit.getCollection(collectionName).createIndex(keys);
        }
    }

    public static int save(String collectionName, List<Record> records) {
        List<DBObject> objs = new ArrayList<DBObject>();
        for (Record record : records) {
            objs.add(toDbObject(record));
        }
        return MongoKit.getCollection(collectionName).insert(objs).getN();
    }

    public static int save(String collectionName, Record record) {
        return MongoKit.getCollection(collectionName).save(toDbObject(record)).getN();
    }

    public static Record findOne(String collectionName, Map<String, Object> q) {
        DBObject dbObject = MongoKit.getCollection(collectionName).findOne(toDBObject(toDBObject(q)));
        return (null != dbObject) ? toRecord(dbObject) : null;
    }

    public static Record findOne(String collectionName, BasicDBObject dbObject) {
        DBObject dbResult = MongoKit.getCollection(collectionName).findOne(toDBObject(toDBObject(dbObject)));
        return (null != dbResult) ? toRecord(dbResult) : null;
    }

    public static Record findOne(String collectionName, Map<String, Object> filter, Map<String, Object> like) {
        BasicDBObject conditons = new BasicDBObject();
        buildFilter(filter, conditons);
        buildLike(like, conditons);
        DBObject dbResult = MongoKit.getCollection(collectionName).findOne(conditons);
        return (null != dbResult) ? toRecord(dbResult) : null;
    }

    public static Record find(String collectionName, Map<String, Object> q) {
        DBCursor dbCursor = MongoKit.getCollection(collectionName).find(toDBObject(q));
        if (dbCursor.size() > 0) {
            return toRecord(dbCursor.next());
        } else {
            return null;
        }
    }

    public static Record find(String collectionName, BasicDBObject dbObject) {
        DBCursor dbCursor = MongoKit.getCollection(collectionName).find(dbObject);
        if (dbCursor.size() > 0) {
            return toRecord(dbCursor.next());
        } else {
            return null;
        }
    }

    public static Record find(String collectionName, Map<String, Object> filter, Map<String, Object> like) {
        BasicDBObject conditons = new BasicDBObject();
        buildFilter(filter, conditons);
        buildLike(like, conditons);
        DBCursor dbCursor = MongoKit.getCollection(collectionName).find(conditons);
        if (dbCursor.size() > 0) {
            return toRecord(dbCursor.next());
        } else {
            return null;
        }
    }

    public static Long count(String collectionName, Map<String, Object> q) {
        Long count = MongoKit.getCollection(collectionName).count(toDBObject(q));
        if (count > 0) {
            return count;
        } else {
            return 0L;
        }
    }

    public static Long count(String collectionName, BasicDBObject dbObject) {
        Long count = MongoKit.getCollection(collectionName).count(dbObject);
        if (count > 0) {
            return count;
        } else {
            return 0L;
        }
    }

    public static Long count(String collectionName, Map<String, Object> filter, Map<String, Object> like) {
        BasicDBObject conditons = new BasicDBObject();
        buildFilter(filter, conditons);
        buildLike(like, conditons);
        Long count = MongoKit.getCollection(collectionName).count(conditons);
        if (count > 0) {
            return count;
        } else {
            return 0L;
        }
    }


    public static List<Record> list(String collection, Map<String, Object> filter, Map<String, Object> like, Map<String, Object> sort) {
        DBCollection logs = MongoKit.getCollection(collection);
        BasicDBObject conditons = new BasicDBObject();
        buildFilter(filter, conditons);
        buildLike(like, conditons);
        DBCursor dbCursor = logs.find(conditons);
        sort(sort, dbCursor);
        List<Record> records = new ArrayList<Record>();
        while (dbCursor.hasNext()) {
            records.add(toRecord(dbCursor.next()));
        }
        return records;
    }

    public static List<Record> list(String collection) {
        return list(collection, null, null, null);
    }

    public static List<Record> list(String collection, Map<String, Object> filter) {
        return list(collection, filter, null, null);
    }

    public static List<Record> list(String collection, Map<String, Object> filter, Map<String, Object> like) {
        return list(collection, filter, like, null);
    }

    public static Page<Record> paginate(String collection, int pageNumber, int pageSize) {
        return paginate(collection, pageNumber, pageSize, null, null, null);
    }

    public static Page<Record> paginate(String collection, int pageNumber, int pageSize, Map<String, Object> filter) {
        return paginate(collection, pageNumber, pageSize, filter, null, null);
    }

    public static Page<Record> paginate(String collection, int pageNumber, int pageSize, Map<String, Object> filter,
                                        Map<String, Object> like) {
        return paginate(collection, pageNumber, pageSize, filter, like, null);
    }

    public static Page<Record> paginate(String collection, int pageNumber, int pageSize, Map<String, Object> filter,
                                        Map<String, Object> like, Map<String, Object> sort) {
        DBCollection logs = MongoKit.getCollection(collection);
        BasicDBObject conditons = new BasicDBObject();
        buildFilter(filter, conditons);
        buildLike(like, conditons);
        DBCursor dbCursor = logs.find(conditons);
        page(pageNumber, pageSize, dbCursor);
        sort(sort, dbCursor);
        List<Record> records = new ArrayList<Record>();
        while (dbCursor.hasNext()) {
            records.add(toRecord(dbCursor.next()));
        }
        int totalRow = dbCursor.count();
        if (totalRow <= 0) {
            return new Page<Record>(new ArrayList<Record>(0), pageNumber, pageSize, 0, 0);
        }
        int totalPage = totalRow / pageSize;
        if (totalRow % pageSize != 0) {
            totalPage++;
        }
        Page<Record> page = new Page<Record>(records, pageNumber, pageSize, totalPage, totalRow);
        return page;
    }

    private static void page(int pageNumber, int pageSize, DBCursor dbCursor) {
        dbCursor = dbCursor.skip((pageNumber - 1) * pageSize).limit(pageSize);
    }

    private static void sort(Map<String, Object> sort, DBCursor dbCursor) {
        if (sort != null) {
            DBObject dbo = new BasicDBObject();
            Set<Entry<String, Object>> entrySet = sort.entrySet();
            for (Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                Object val = entry.getValue();
                dbo.put(key, "asc".equalsIgnoreCase(val + "") ? 1 : -1);
            }
            dbCursor = dbCursor.sort(dbo);
        }
    }

    private static void buildLike(Map<String, Object> like, BasicDBObject conditons) {
        if (like != null) {
            Set<Entry<String, Object>> entrySet = like.entrySet();
            for (Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                Object val = entry.getValue();
                conditons.put(key, MongoKit.getLikeStr(val));
            }
        }
    }

    private static void buildFilter(Map<String, Object> filter, BasicDBObject conditons) {
        if (filter != null) {
            Set<Entry<String, Object>> entrySet = filter.entrySet();
            for (Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                Object val = entry.getValue();
                conditons.put(key, val);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Record toRecord(DBObject dbObject) {
        Record record = new Record();
        record.setColumns(dbObject.toMap());
        return record;
    }

    public static BasicDBObject getLikeStr(Object findStr) {
        Pattern pattern = Pattern.compile("^.*" + findStr + ".*$", Pattern.CASE_INSENSITIVE);
        return new BasicDBObject("$regex", pattern);
    }

    public static DB getDB() {
        return defaultDb;
    }

    public static DB getDB(String dbName) {
        return client.getDB(dbName);
    }

    public static DBCollection getCollection(String name) {
        return defaultDb.getCollection(name);
    }

    public static DBCollection getDBCollection(String dbName, String collectionName) {
        return getDB(dbName).getCollection(collectionName);
    }

    public static MongoClient getClient() {
        return client;
    }

    public static void setMongoClient(MongoClient client) {
        MongoKit.client = client;
    }

    private static BasicDBObject toDBObject(Map<String, Object> map) {
        BasicDBObject dbObject = new BasicDBObject();
        Set<Entry<String, Object>> entrySet = map.entrySet();
        for (Entry<String, Object> entry : entrySet) {
            String key = entry.getKey();
            Object val = entry.getValue();
            dbObject.append(key, val);
        }
        return dbObject;
    }

    private static BasicDBObject toDbObject(Record record) {
        BasicDBObject object = new BasicDBObject();
        for (Entry<String, Object> e : record.getColumns().entrySet()) {
            object.append(e.getKey(), e.getValue());
        }
        return object;
    }
}