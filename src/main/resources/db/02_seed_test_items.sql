-- =====================================================================
--  검사 항목 사전 시딩
--
--  ⚠️ 정상범위는 개발 착수용 초안입니다.
--     서비스 오픈 전 산부인과 또는 진단검사의학과 검수를 반드시 받으세요.
--
--  is_pregnancy_specific = TRUE 인 항목(★)은 임신 중 정상범위가
--  비임신 성인과 크게 달라, 검사지에 인쇄된 참고치를 쓰면
--  정상인 산모를 이상으로 표시하게 됩니다.
-- =====================================================================

DELETE FROM test_item_catalog;

INSERT INTO test_item_catalog
(code, name_ko, name_en, category, result_type, unit,
 normal_min, normal_max, normal_text, hard_limit_min, hard_limit_max,
 caution_margin_ratio, is_pregnancy_specific, is_trendable, name_variants,
 brief_for_mom, display_order)
VALUES

-- ===== 혈액 (CBC) =====
('HB','혈색소','Hemoglobin','HEMATOLOGY','NUMBER','g/dL',
 11.0,15.0,NULL,0.1,30,0.100,TRUE,TRUE,
 '["혈색소","헤모글로빈","혈색소(헤모글로빈)","Hb","HGB","Hemoglobin"]',
 '혈색소는 혈액 속에서 산소를 몸 곳곳으로 운반해주는 단백질이에요.',1),

('HCT','헤마토크리트','Hematocrit','HEMATOLOGY','NUMBER','%',
 33.0,44.0,NULL,5,70,0.100,TRUE,TRUE,
 '["헤마토크리트","적혈구용적률","Hct","HCT","Hematocrit"]',
 '전체 혈액 중에서 적혈구가 차지하는 비율이에요.',2),

('RBC','적혈구수','RBC','HEMATOLOGY','NUMBER','M/µL',
 3.5,5.0,NULL,0.5,10,0.100,TRUE,TRUE,
 '["적혈구수","적혈구","적혈구수(RBC)","RBC","Red Blood Cell"]',
 '산소를 실어 나르는 적혈구의 개수예요.',3),

('WBC','백혈구수','WBC','HEMATOLOGY','NUMBER','K/µL',
 4.0,16.9,NULL,0.1,100,0.100,TRUE,TRUE,
 '["백혈구수","백혈구","백혈구수(WBC)","WBC","White Blood Cell"]',
 '몸을 지키는 면역 세포예요. 임신 중에는 자연스럽게 늘어납니다.',4),

('PLT','혈소판수','Platelet','HEMATOLOGY','NUMBER','K/µL',
 130,400,NULL,1,2000,0.100,FALSE,TRUE,
 '["혈소판수","혈소판","혈소판수(Platelet)","PLT","Platelet"]',
 '피가 났을 때 굳게 해주는 세포예요.',5),

('MCV','평균적혈구용적','MCV','HEMATOLOGY','NUMBER','fL',
 80,100,NULL,40,150,0.100,FALSE,TRUE,
 '["평균적혈구용적","MCV"]',
 '적혈구 하나의 평균 크기예요. 빈혈의 원인을 구분할 때 봅니다.',6),

('MCH','평균적혈구혈색소량','MCH','HEMATOLOGY','NUMBER','pg',
 27,33,NULL,10,50,0.100,FALSE,TRUE,
 '["평균적혈구혈색소량","MCH"]',
 '적혈구 하나에 들어 있는 혈색소의 평균량이에요.',7),

('MCHC','평균적혈구혈색소농도','MCHC','HEMATOLOGY','NUMBER','g/dL',
 32,36,NULL,20,50,0.100,FALSE,TRUE,
 '["평균적혈구혈색소농도","MCHC"]',
 '적혈구 안의 혈색소 농도예요.',8),

('ABO','ABO 혈액형','ABO Blood Type','HEMATOLOGY','TEXT',NULL,
 NULL,NULL,NULL,NULL,NULL,0.100,FALSE,FALSE,
 '["ABO 혈액형검사","ABO 혈액형","A,B,O 혈액형검사","혈액형","ABO"]',
 'A·B·O·AB 중 어떤 혈액형인지 확인하는 검사예요.',9),

('RH','Rh 혈액형','Rh Type','HEMATOLOGY','TEXT',NULL,
 NULL,NULL,NULL,NULL,NULL,0.100,FALSE,FALSE,
 '["Rh 혈액형검사","Rho,D형혈액형검사","Rh-Ir","RH","Rh Type"]',
 'Rh 음성이면 임신 중 특별한 관리가 필요할 수 있어요.',10),

-- ===== 철 대사 =====
('FERRITIN','페리틴','Ferritin','NUTRITION','NUMBER','ng/mL',
 30,200,NULL,1,5000,0.100,TRUE,TRUE,
 '["페리틴","저장철","Ferritin"]',
 '몸에 저장해둔 철분의 양을 보여줘요. 임신 중에는 30 미만이면 철분이 부족한 편이에요.',11),

('IRON','혈청철','Serum Iron','NUTRITION','NUMBER','µg/dL',
 50,170,NULL,5,600,0.100,FALSE,TRUE,
 '["혈청철","철","Serum Iron","Fe"]',
 '지금 혈액 속을 돌고 있는 철분의 양이에요.',12),

('TIBC','총철결합능','TIBC','NUTRITION','NUMBER','µg/dL',
 250,450,NULL,50,1000,0.100,FALSE,TRUE,
 '["총철결합능","TIBC"]',
 '철분을 실어 나를 수 있는 여유 공간을 나타내요.',13),

-- ===== 간 기능 =====
('AST','AST','AST(SGOT)','CHEMISTRY','NUMBER','IU/L',
 0,40,NULL,1,10000,0.100,FALSE,TRUE,
 '["AST","AST(SGOT)","SGOT","GOT","AST/SGOT"]',
 '간 세포에 들어 있는 효소로, 간 건강을 보는 대표 수치예요.',20),

('ALT','ALT','ALT(SGPT)','CHEMISTRY','NUMBER','IU/L',
 0,40,NULL,1,10000,0.100,FALSE,TRUE,
 '["ALT","ALT(SGPT)","SGPT","GPT","ALT/SGPT"]',
 'AST와 함께 간 상태를 확인하는 수치예요.',21),

('ALP','알칼리인산분해효소','ALP','CHEMISTRY','NUMBER','IU/L',
 40,250,NULL,5,3000,0.100,TRUE,TRUE,
 '["알칼리인산분해효소","알칼리성인산분해효소","ALP","Alkaline Phosphatase"]',
 '임신 중에는 태반에서도 만들어져서 자연스럽게 높아져요.',22),

('GGT','감마지티피','GGT','CHEMISTRY','NUMBER','U/L',
 6,42,NULL,1,3000,0.100,FALSE,TRUE,
 '["감마지티피","GGT","r-GTP","감마GTP"]',
 '간과 담도 상태를 함께 보는 수치예요.',23),

('TBIL','총빌리루빈','Total Bilirubin','CHEMISTRY','NUMBER','mg/dL',
 0.2,1.2,NULL,0.01,50,0.100,FALSE,TRUE,
 '["총빌리루빈","빌리루빈","T-Bil","Total Bilirubin"]',
 '적혈구가 분해되면서 나오는 색소로, 간의 처리 능력을 봐요.',24),

('TP','총단백','Total Protein','CHEMISTRY','NUMBER','g/dL',
 6.0,8.0,NULL,1,15,0.100,TRUE,TRUE,
 '["총단백","총단백질","TP","Total Protein"]',
 '임신 중에는 혈액이 묽어지면서 조금 낮아질 수 있어요.',25),

('ALB','알부민','Albumin','CHEMISTRY','NUMBER','g/dL',
 2.8,4.5,NULL,0.5,8,0.100,TRUE,TRUE,
 '["알부민","Alb","Albumin"]',
 '혈액 속 대표 단백질이에요. 임신 중에는 자연스럽게 낮아져요.',26),

-- ===== 신장 기능 =====
('BUN','혈중요소질소','BUN','CHEMISTRY','NUMBER','mg/dL',
 3,13,NULL,1,300,0.100,TRUE,TRUE,
 '["혈중요소질소","요소질소","BUN"]',
 '신장이 노폐물을 걸러내는 정도를 봐요. 임신 중에는 낮아지는 게 정상이에요.',30),

('CRE','크레아티닌','Creatinine','CHEMISTRY','NUMBER','mg/dL',
 0.4,0.8,NULL,0.05,20,0.100,TRUE,TRUE,
 '["크레아티닌","크레아닌","Cr","Creatinine"]',
 '신장 기능을 보는 대표 수치예요. 임신 중에는 낮아지는 게 정상이에요.',31),

('UA','요산','Uric Acid','CHEMISTRY','NUMBER','mg/dL',
 2.5,6.0,NULL,0.1,30,0.100,FALSE,TRUE,
 '["요산","Uric Acid","UA"]',
 '몸에서 만들어지는 노폐물의 하나예요.',32),

-- ===== 혈당 =====
('GLU','공복혈당','Glucose','CHEMISTRY','NUMBER','mg/dL',
 70,99,NULL,10,900,0.100,FALSE,TRUE,
 '["공복혈당","혈당","포도당","Glucose","FBS"]',
 '아무것도 먹지 않은 상태에서 잰 혈액 속 당의 양이에요.',35),

('GCT50','임신성 당뇨 선별검사','GCT 50g','CHEMISTRY','NUMBER','mg/dL',
 0,140,NULL,10,900,0.100,FALSE,TRUE,
 '["임신성당뇨선별검사","임당검사","GCT","50g 경구당부하","당부하검사"]',
 '설탕물을 마시고 한 시간 뒤 혈당을 재서 임신성 당뇨 가능성을 봐요.',36),

('HBA1C','당화혈색소','HbA1c','CHEMISTRY','NUMBER','%',
 4.0,5.6,NULL,2,20,0.100,FALSE,TRUE,
 '["당화혈색소","HbA1c","A1c"]',
 '최근 2~3개월간의 평균 혈당을 보여줘요.',37),

-- ===== 지질 =====
('CHOL','총콜레스테롤','Total Cholesterol','CHEMISTRY','NUMBER','mg/dL',
 150,300,NULL,20,1000,0.100,TRUE,TRUE,
 '["총콜레스테롤","콜레스테롤","T-CHOL","Total Cholesterol"]',
 '임신 중에는 자연스럽게 올라가요.',40),

('TG','중성지방','Triglyceride','CHEMISTRY','NUMBER','mg/dL',
 50,300,NULL,10,5000,0.100,TRUE,TRUE,
 '["중성지방","트리글리세라이드","TG","Triglyceride"]',
 '임신 중에는 2~3배까지 올라가는 게 흔해요.',41),

('HDL','HDL 콜레스테롤','HDL','CHEMISTRY','NUMBER','mg/dL',
 40,100,NULL,5,200,0.100,FALSE,TRUE,
 '["HDL","HDL콜레스테롤","좋은콜레스테롤"]',
 '혈관에 쌓인 기름을 치워주는 좋은 콜레스테롤이에요.',42),

('LDL','LDL 콜레스테롤','LDL','CHEMISTRY','NUMBER','mg/dL',
 0,160,NULL,5,500,0.100,FALSE,TRUE,
 '["LDL","LDL콜레스테롤","나쁜콜레스테롤"]',
 '많으면 혈관에 쌓일 수 있는 콜레스테롤이에요.',43),

-- ===== 감염 =====
('HBSAG','B형간염 표면항원','HBsAg','IMMUNO_SEROLOGY','MIXED',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["B형간염표면항원","HBsAg","HBs Ag","B형간염표면항원-HBs Ag"]',
 '지금 B형간염 바이러스에 감염되어 있는지 확인하는 검사예요.',50),

('HBSAB','B형간염 표면항체','HBsAb','IMMUNO_SEROLOGY','MIXED',NULL,
 NULL,NULL,'양성',NULL,NULL,0.100,FALSE,FALSE,
 '["B형간염표면항체","HBsAb","HBs Ab","B형간염표면항체-HBs Ab","Anti-HBs"]',
 'B형간염을 막아주는 항체가 있는지 보는 검사예요. 있는 편이 좋아요.',51),

('HCVAB','C형간염 항체','Anti-HCV','IMMUNO_SEROLOGY','MIXED',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["C형간염항체","Anti-HCV","HCV Ab","C형간염"]',
 'C형간염 바이러스 감염 여부를 확인해요.',52),

('VDRL','매독반응검사','RPR/VDRL','IMMUNO_SEROLOGY','TEXT',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["매독반응검사","매독반응검사 RPR","RPR","VDRL","매독혈청검사"]',
 '매독 감염 여부를 확인하는 선별 검사예요.',53),

('TPHA','매독항체검사','TPHA','IMMUNO_SEROLOGY','TEXT',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["매독항체검사","TPHA","TPLA","매독확인검사"]',
 '매독 선별검사 결과를 확인하는 검사예요.',54),

('HIV','HIV 항원/항체','HIV Ag/Ab','IMMUNO_SEROLOGY','MIXED',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["후천성면역결핍","HIV","HIV 항원/항체","정밀면역검사-HIV 항원/항체 동시 선별","Anti-HIV"]',
 'HIV 감염 여부를 확인하는 검사예요.',55),

('RUB_IGG','풍진 IgG 항체','Rubella IgG','IMMUNO_SEROLOGY','TEXT',NULL,
 NULL,NULL,'양성',NULL,NULL,0.100,FALSE,FALSE,
 '["풍진IgG","Rubella IgG","IgG-Rubella","풍진항체 IgG","바이러스항체,정밀-IgG-Rubella"]',
 '과거에 풍진 면역이 생겼는지 확인해요. 양성이면 면역이 있다는 뜻이에요.',56),

('RUB_IGM','풍진 IgM 항체','Rubella IgM','IMMUNO_SEROLOGY','TEXT',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["풍진IgM","Rubella IgM","IgM-Rubella","풍진항체 IgM","바이러스항체,정밀-IgM-Rubella"]',
 '최근에 풍진에 감염됐는지 확인해요. 음성이 정상이에요.',57),

-- ===== 소변 =====
('U_PROT','요단백','Urine Protein','URINALYSIS','TEXT',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["요단백","단백뇨","소변단백","Urine Protein","Protein"]',
 '소변에 단백질이 섞여 나오는지 봐요. 임신 중에는 특히 중요한 검사예요.',60),

('U_GLU','요당','Urine Glucose','URINALYSIS','TEXT',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["요당","소변당","Urine Glucose","Glucose(urine)"]',
 '소변으로 당이 빠져나오는지 확인해요.',61),

('U_BLOOD','요잠혈','Urine Blood','URINALYSIS','TEXT',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["요잠혈","잠혈","혈뇨","Urine Blood","Occult Blood"]',
 '눈에 보이지 않는 혈액이 소변에 섞여 있는지 봐요.',62),

('U_KET','요케톤','Urine Ketone','URINALYSIS','TEXT',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["요케톤","케톤","케톤체","Urine Ketone","Ketone"]',
 '몸이 에너지를 지방에서 끌어 쓰고 있는지 보여줘요.',63),

('U_LEU','요백혈구','Urine Leukocyte','URINALYSIS','TEXT',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["요백혈구","백혈구(소변)","Leukocyte","WBC(urine)"]',
 '소변에 염증 세포가 있는지 확인해요.',64),

('U_NIT','요아질산염','Urine Nitrite','URINALYSIS','TEXT',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["요아질산염","아질산염","Nitrite"]',
 '소변에 세균이 있는지 짐작할 수 있는 항목이에요.',65),

('U_PH','요산도','Urine pH','URINALYSIS','NUMBER',NULL,
 4.5,8.0,NULL,1,14,0.100,FALSE,TRUE,
 '["요산도","소변pH","pH","Urine pH"]',
 '소변이 산성인지 알칼리성인지 나타내요.',66),

-- ===== 갑상선 =====
('TSH','갑상선자극호르몬','TSH','ENDOCRINE','NUMBER','mIU/mL',
 0.1,4.0,NULL,0.001,200,0.100,TRUE,TRUE,
 '["갑상선자극호르몬","TSH","티에스에이치"]',
 '갑상선이 잘 작동하는지 보는 수치예요. 임신 시기에 따라 기준이 달라져요.',70),

('FT4','유리 티록신','Free T4','ENDOCRINE','NUMBER','ng/dL',
 0.8,1.8,NULL,0.01,20,0.100,FALSE,TRUE,
 '["유리티록신","Free T4","FT4","F-T4"]',
 '갑상선이 만드는 호르몬의 양이에요.',71),

-- ===== 영양 =====
('VIT_D','비타민 D','Vitamin D 25-OH','NUTRITION','NUMBER','ng/mL',
 30,100,NULL,1,300,0.100,FALSE,TRUE,
 '["비타민D","비타민 D","Vitamin D","25-OH Vitamin D","비타민디"]',
 '뼈를 튼튼하게 하고 면역에도 관여해요. 우리나라 사람은 부족한 경우가 많아요.',75),

('FOLATE','엽산','Folate','NUTRITION','NUMBER','ng/mL',
 3.0,20.0,NULL,0.1,100,0.100,FALSE,TRUE,
 '["엽산","폴산","Folate","Folic Acid"]',
 '아기의 신경관이 만들어질 때 꼭 필요한 영양소예요.',76),

('VIT_B12','비타민 B12','Vitamin B12','NUTRITION','NUMBER','pg/mL',
 200,900,NULL,10,5000,0.100,FALSE,TRUE,
 '["비타민B12","비타민 B12","Vitamin B12","코발라민"]',
 '피를 만들고 신경이 제 역할을 하도록 돕는 영양소예요.',77),

-- ===== 영상 =====
('CXR','흉부 촬영','Chest PA','IMAGING','TEXT',NULL,
 NULL,NULL,'정상',NULL,NULL,0.100,FALSE,FALSE,
 '["흉부촬영","흉부 X선","Chest PA","흉부[Chest PA]","폐결핵","흉부 [Chest PA](폐결핵)"]',
 '폐와 심장의 모양을 보는 X선 검사예요.',80);
