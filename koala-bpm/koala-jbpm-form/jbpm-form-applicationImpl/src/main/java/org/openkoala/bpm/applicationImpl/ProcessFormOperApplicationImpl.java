package org.openkoala.bpm.applicationImpl;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.openkoala.bpm.application.ProcessFormOperApplication;
import org.openkoala.bpm.application.dto.DynaProcessFormDTO;
import org.openkoala.bpm.application.dto.DynaProcessKeyDTO;
import org.openkoala.bpm.application.dto.DynaProcessTemplateDTO;
import org.openkoala.bpm.application.dto.ProcessDTO;
import org.openkoala.bpm.application.dto.SelectOptions;
import org.openkoala.bpm.application.utils.EntityTurnToDTOUtil;
import org.openkoala.bpm.processdyna.core.DynaProcessForm;
import org.openkoala.bpm.processdyna.core.DynaProcessKey;
import org.openkoala.bpm.processdyna.core.DynaProcessTemplate;
import org.openkoala.bpm.processdyna.core.DynaType;
import org.openkoala.bpm.processdyna.core.FieldType;
import org.openkoala.bpm.processdyna.core.ValidateRule;
import org.openkoala.exception.base.BaseException;
import org.openkoala.jbpm.wsclient.JBPMApplication;
import org.openkoala.jbpm.wsclient.JBPMApplicationImplService;
import org.openkoala.jbpm.wsclient.ProcessVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.dayatang.domain.InstanceFactory;
import com.dayatang.querychannel.service.QueryChannelService;
import com.dayatang.querychannel.support.Page;


@Transactional
@Named
public class ProcessFormOperApplicationImpl implements
		ProcessFormOperApplication {
	
	private static final Logger LOG = LoggerFactory.getLogger(ProcessFormOperApplicationImpl.class);
	
	
	
	private static QueryChannelService queryChannel;
	
	private static QueryChannelService getQueryChannelService(){
		if(queryChannel==null){
			queryChannel = InstanceFactory.getInstance(QueryChannelService.class);
		}
		return queryChannel;
	}
	
	
    private JBPMApplication jbpmApplication;
	
	private JBPMApplication getJBPMApplication() { 
		if(jbpmApplication==null){
			try { 
				Configuration conf = new PropertiesConfiguration("jbpmService.properties");
				String wsdlURL = conf.getString("wsdlURL");
				LOG.info("JBPM WS:{}",wsdlURL);
				jbpmApplication = new JBPMApplicationImplService(wsdlURL).getJBPMApplicationImplPort(); 
			} catch (Exception e) {
				e.printStackTrace(); 
			} 
		} 
		return jbpmApplication; 
	}

	public void createDynaProcessForm(DynaProcessFormDTO form) {
		DynaProcessForm processForm =new DynaProcessForm(form.getProcessId(),form.getBizName());
		
		if(DynaProcessForm.queryActiveDynaProcessFormByProcessId(form.getProcessId()) != null){
			throw new BaseException("form_is_exists", "该流程已经绑定表单");
		}
		
		List<DynaProcessKeyDTO> processKeys = form.getProcessKeys();
		for (DynaProcessKeyDTO key : processKeys) {
			DynaProcessKey processKey = new DynaProcessKey(key.getKeyId(), key.getKeyName(), key.getKeyType());
			processKey.setRequired("true".equals(key.getRequired()));
			processKey.setInnerVariable("true".equals(key.getInnerVariable()));
			processKey.setValidationExpr(key.getValidationExpr());
			processKey.setValidationType(key.getValidationType());
			processKey.setShowOrder(key.getShowOrder());
			processKey.setOutputVar("true".equals(key.getOutputVar()));
			processKey.setKeyOptions(key.getKeyOptions());
			processKey.setValOutputType(key.getValOutputType());
			processKey.setDynaTable(processForm);
			processForm.addDynaProcessKey(processKey);
		}
		
		if(form.getTemplateId()>0){
			DynaProcessTemplate template = DynaProcessTemplate.getRepository().get(DynaProcessTemplate.class, form.getTemplateId());
            processForm.setTemplate(template);
		}
		processForm.setProcessId(form.getProcessId());
		processForm.setBizDescription(form.getBizDescription());
		//该字段暂时全部为true
		//processForm.setActive("true".equals(form.getActive()));
		processForm.setActive(true);
		processForm.save();
		
	}

	public Page<DynaProcessFormDTO> queryDynaProcessFormsByPage(DynaProcessFormDTO search,int currentPage, int pageSize) {
		
        List<DynaProcessFormDTO> datas = new ArrayList<DynaProcessFormDTO>();
		String jpql = "select df from DynaProcessForm df join df.template as dt";
		Page<DynaProcessForm> page = getQueryChannelService().queryPagedResultByPageNo(jpql, new Object[0], currentPage, pageSize);
		
		for (DynaProcessForm processForm : page.getResult()) {
			processForm.setKeys(null);
//			datas.add(DynaProcessForm2DTO(processForm));
			datas.add(EntityTurnToDTOUtil.DynaProcessForm2DTO(processForm));
			
		}
		long start = (page.getCurrentPageNo() - 1)*pageSize;
		return new Page<DynaProcessFormDTO>(start, page.getTotalPageCount(), pageSize, datas);
	}


	public void publishProcessTemplate(DynaProcessTemplateDTO template) {
		
	}

	public DynaProcessTemplateDTO getDynaProcessTemplate(String templateName) {
		return null;
	}

	public List<SelectOptions> getDataTypeList() {
		List<SelectOptions> list = new ArrayList<SelectOptions>();
		for (DynaType e : DynaType.values()) {
			list.add(new SelectOptions(e.getText(), e.name()));
		}
		return list;
	}

	public List<DynaProcessTemplateDTO> getActiveProcessTemplates() {
		List<DynaProcessTemplateDTO> datas = new ArrayList<DynaProcessTemplateDTO>();
		List<DynaProcessTemplate> list = DynaProcessTemplate.getRepository().findAll(DynaProcessTemplate.class);
		for (DynaProcessTemplate template : list) {
			DynaProcessTemplateDTO dto = new DynaProcessTemplateDTO(template.getTemplateName(), template.getTemplateDescription(), template.getTemplateData());
			dto.setId(template.getId());
			datas.add(dto);
		}
		return datas;
	}

	public List<ProcessDTO> getActiveProcesses() {
		List<ProcessDTO> datas = new ArrayList<ProcessDTO>();
		
		try {
			List<ProcessVO> processes = getJBPMApplication().getProcesses();
			if(processes == null)return datas;
			String jpql = "select df.processId from DynaProcessForm df";
			//已绑定form的流程列表
			List<String> existsProcessIds = getQueryChannelService().queryResult(jpql, new Object[0]);
			for (ProcessVO process : processes) {
				//排除已绑定的流程
				if(existsProcessIds == null || !existsProcessIds.contains(process.getId())){
					datas.add(new ProcessDTO(process.getId(), process.getName()));
				}
			}
			//TODO
			//datas.add(new ProcessDTO("qingjia", "请假"));
			//datas.add(new ProcessDTO("jiaban", "加班"));
			//datas.add(new ProcessDTO("yuzhi", "预支"));
		} catch (Exception e) {
			// TODO: handle exception
			//datas.add(new ProcessDTO("qingjia", "请假"));
			LOG.error("load process list", e);
		}
		
		return datas;
	}

	public List<SelectOptions> getValidateRules() {
		List<SelectOptions> list = new ArrayList<SelectOptions>();
		for (ValidateRule e : ValidateRule.values()) {
			list.add(new SelectOptions(e.getText(), e.name()));
		}
		return list;
	}
	
	public List<SelectOptions> getFieldValTypeList() {
		List<SelectOptions> list = new ArrayList<SelectOptions>();
		for (FieldType e : FieldType.values()) {
			list.add(new SelectOptions(e.getText(), e.name()));
		}
		return list;
	}

	public DynaProcessFormDTO getDynaProcessFormById(Long id) {
		String jpql = "from DynaProcessForm fetch all properties where id = ?";
		List<DynaProcessForm> list = getQueryChannelService().queryResult(jpql, new Object[]{id});
		if(list != null && list.size()>0){
			DynaProcessForm processForm = list.get(0);
//			return DynaProcessForm2DTO(processForm);
			return EntityTurnToDTOUtil.DynaProcessForm2DTO(processForm);
		}
		
		return null;
	}


	public void deleteDynaProcessFormById(Long[] ids) {
		StringBuffer sb = new StringBuffer("(");
		for (Long id : ids) {
			sb.append(id).append(",");
		}
		sb.deleteCharAt(sb.length() - 1).append(")");
		String jpql = "from DynaProcessForm fetch all properties where id in" + sb.toString();
		List<DynaProcessForm> list = getQueryChannelService().queryResult(jpql, new Object[0]);
		for (DynaProcessForm dynaProcessForm : list) {
			dynaProcessForm.remove();
		}
		
	}

	
	
}