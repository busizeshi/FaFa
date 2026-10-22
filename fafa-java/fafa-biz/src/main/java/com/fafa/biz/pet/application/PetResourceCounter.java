package com.fafa.biz.pet.application;
import java.util.Map;
/** 删除预览的领域计数扩展；提供方只查询数据，不执行删除或回调宠物用例。 */
public interface PetResourceCounter {
    Map<String,Long> countForPet(String petId);
}
