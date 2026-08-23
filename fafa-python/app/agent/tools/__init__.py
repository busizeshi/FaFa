"""
AI 数据查询工具链

工具清单与产品文档第十一章保持一致，全部经 HTTP 回调 fafa-java 只读接口取数：
getPetProfile / getWeightHistory / getFeedingHistory
getWaterHistory / getDefecationHistory / getEventHistory
getVaccineHistory / getDewormingHistory / searchPetPhotos
getFoodInfo / getReminderHistory / getPetReports
getMedicalHistory / getMedicationHistory / getExpenseHistory

TODO P1：以 LangChain Tool 形式逐个封装，统一注入 traceId 与内部令牌。
"""
