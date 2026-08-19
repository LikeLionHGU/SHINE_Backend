-- 실제 검사지(GC Labs 등)에서 미분류로 떨어지던 항목 보강
-- 02_seed_test_items.sql 이후에 실행한다

INSERT IGNORE INTO test_item_catalog
(code, name_ko, name_en, category, result_type, unit,
 normal_min, normal_max, normal_text, hard_limit_min, hard_limit_max,
 caution_margin_ratio, is_pregnancy_specific, is_trendable, name_variants,
 brief_for_mom, display_order)
VALUES
('NEUT_PCT','호중구 비율','Neutrophil %','HEMATOLOGY','NUMBER','%',
 40,80,NULL,0,100,0.100,TRUE,TRUE,
 '["호중구","호중구비율","NEUT%","NEUT","Neutrophil","Neutrophils %","Segmented Neutrophil"]',
 '세균과 싸우는 백혈구예요. 임신 중에는 자연스럽게 늘어납니다.',90),

('LYMPH_PCT','림프구 비율','Lymphocyte %','HEMATOLOGY','NUMBER','%',
 15,45,NULL,0,100,0.100,TRUE,TRUE,
 '["림프구","림프구비율","LYMPH%","LYMPH","Lymphocyte","Lymphocytes %"]',
 '바이러스에 대응하는 백혈구예요. 임신 중에는 비율이 조금 낮아집니다.',91),

('MONO_PCT','단핵구 비율','Monocyte %','HEMATOLOGY','NUMBER','%',
 2,10,NULL,0,100,0.100,FALSE,TRUE,
 '["단핵구","단핵구비율","MONO%","MONO","Monocyte","Monocytes %"]',
 '오래된 세포와 세균을 치우는 백혈구예요.',92),

('EOS_PCT','호산구 비율','Eosinophil %','HEMATOLOGY','NUMBER','%',
 0,7,NULL,0,100,0.100,FALSE,TRUE,
 '["호산구","호산구비율","EO%","EOS","Eosinophil","Eosinophils %"]',
 '알레르기 반응에 관여하는 백혈구예요.',93),

('BASO_PCT','호염기구 비율','Basophil %','HEMATOLOGY','NUMBER','%',
 0,2,NULL,0,100,0.100,FALSE,TRUE,
 '["호염기구","호염기구비율","BA%","BASO","Basophil","Basophils %"]',
 '가장 수가 적은 백혈구로, 알레르기와 관련이 있어요.',94),

('RDW_CV','적혈구 분포폭','RDW-CV','HEMATOLOGY','NUMBER','%',
 11.5,14.5,NULL,5,40,0.100,FALSE,TRUE,
 '["적혈구분포폭","RDW-CV","RDW CV","RDW"]',
 '적혈구 크기가 얼마나 고른지 보여줘요. 빈혈 원인을 가릴 때 봅니다.',95),

('RDW_SD','적혈구 분포폭(SD)','RDW-SD','HEMATOLOGY','NUMBER','fL',
 39,46,NULL,10,120,0.100,FALSE,TRUE,
 '["적혈구분포폭SD","RDW-SD","RDW SD"]',
 '적혈구 크기 편차를 다른 방식으로 나타낸 값이에요.',96),

('U_BILI','요빌리루빈','Urine Bilirubin','URINALYSIS','TEXT',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["요빌리루빈","소변빌리루빈","Bilirubin(RU)","Bilirubin (RU)","Urine Bilirubin","Bilirubin"]',
 '소변에 담즙 색소가 섞여 나오는지 봐요.',97),

('U_UROBIL','요우로빌리노겐','Urobilinogen','URINALYSIS','TEXT',NULL,
 NULL,NULL,'음성',NULL,NULL,0.100,FALSE,FALSE,
 '["요우로빌리노겐","우로빌리노겐","Urobilinogen(RU)","Urobilinogen (RU)","Urobilinogen","URO"]',
 '간과 담도의 상태를 소변으로 확인하는 항목이에요.',98),

('U_SG','요비중','Specific Gravity','URINALYSIS','NUMBER',NULL,
 1.005,1.030,NULL,1.000,1.060,0.100,FALSE,TRUE,
 '["요비중","소변비중","Specific Gravity(RU)","Specific Gravity (RU)","Specific Gravity","SG"]',
 '소변이 얼마나 진한지 나타내요. 수분 섭취량에 따라 달라집니다.',99),

('U_SED','요침사','Urine Sediment','URINALYSIS','TEXT',NULL,
 NULL,NULL,'정상',NULL,NULL,0.100,FALSE,FALSE,
 '["요침사","요침사검사","요침사(Flow cytometry)","Urine Sediment","Sediment"]',
 '소변을 현미경으로 봐서 세포나 결정이 있는지 확인해요.',100);


-- 이미 있는 항목의 병원별 표기 흡수 (설계결정 ④ — 코드 if문이 아니라 데이터로)

UPDATE test_item_catalog SET name_variants = JSON_ARRAY(
  '매독반응검사','매독반응검사 RPR','RPR','RPR(정밀)','RPR (정밀)','VDRL','매독혈청검사')
WHERE code='VDRL';

UPDATE test_item_catalog SET name_variants = JSON_ARRAY(
  '후천성면역결핍','HIV','HIV 항원/항체','AIDS Ag/Ab(Combo)','AIDS Ag/Ab (Combo)',
  'AIDS Ag/Ab','정밀면역검사-HIV 항원/항체 동시 선별','Anti-HIV')
WHERE code='HIV';

UPDATE test_item_catalog SET name_variants = JSON_ARRAY(
  'ABO 혈액형검사','ABO 혈액형','A,B,O 혈액형검사','ABO Cell typing','ABO cell typing',
  'ABO Typing','혈액형','ABO')
WHERE code='ABO';

UPDATE test_item_catalog SET name_variants = JSON_ARRAY(
  'Rh 혈액형검사','Rho,D형혈액형검사','Rh typing','Rh Typing','Rh-Ir','RH','Rh Type')
WHERE code='RH';

UPDATE test_item_catalog SET name_variants = JSON_ARRAY(
  '요케톤','케톤','케톤체','Ketone(RU)','Ketone (RU)','Urine Ketone','Ketone')
WHERE code='U_KET';

UPDATE test_item_catalog SET name_variants = JSON_ARRAY(
  '요아질산염','아질산염','Nitrite(RU)','Nitrite (RU)','Nitrite')
WHERE code='U_NIT';

UPDATE test_item_catalog SET name_variants = JSON_ARRAY(
  '요단백','단백뇨','소변단백','Protein(RU)','Protein (RU)','Urine Protein','Protein')
WHERE code='U_PROT';

UPDATE test_item_catalog SET name_variants = JSON_ARRAY(
  '요당','소변당','Glucose(RU)','Glucose (RU)','Urine Glucose','Glucose(urine)')
WHERE code='U_GLU';

UPDATE test_item_catalog SET name_variants = JSON_ARRAY(
  '요잠혈','잠혈','혈뇨','Blood(RU)','Blood (RU)','Urine Blood','Occult Blood')
WHERE code='U_BLOOD';
