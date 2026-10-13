--liquibase formatted sql

--changeset auto:latest-data runOnChange:true
--comment: Automatically exported latest database records
--
-- PostgreSQL database dump
--


-- Dumped from database version 15.19
-- Dumped by pg_dump version 15.19

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.users VALUES (5, 'worker3', '$2a$10$dOg7zwVv.OR1VeTCSuSopeYepgo1z3juruXS4TS93s2oCi1xWnfgW', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (6, 'admin2', '$2a$10$XYQJxhmtc7U6V5Y2EreoyeTns489iBm93.UHcnEJuyMSstickmboG', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (7, 'admin3', '$2a$10$nfkqR.CIqc8WsPEZ8qTUY.kwhuMN37nGmS.yctrh3LtnH.feoi3v.', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (8, 'admin4', '$2a$10$DUTJ.WwVatSXUj17l9CGROilthIMxO1sO70f2AMsGp0F1.wtOaMNe', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (9, 'admin5', '$2a$10$v3yn/WsOZTGmcPmkXQGO/erGKXYkBxmtAvqX4bmT0vr17K.yCJuOC', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (1, 'superadmin', '$2a$10$e.WSJmZHFspH5wVXYakqTeEtLbo8PLmwbMhXyCoBMjGwRQtpVTvHG', 'superadmin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (2, 'admin1', '$2a$10$acwzqFcD1eE/OU1JDo/khO9V7CaE33JsFFV4iHHky6nX6B7VWQSZC', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (3, 'worker1', '$2a$10$wU895Nz3uR1QpZSV6uBltOg2zmBx1qxWZGKoardAKZXtkCAtzlazG', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (4, 'worker2', '$2a$10$LZ0h5RQbKN7Lqu36m8qEIetOzLnjke8LMoDfpX8ECItduEbD7i14i', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (10, 'admin6', '$2a$10$9yQ8rywn8hCBsnFKtsNxk.5GTHzREYoVf4gCB6xAKp4R4GikSZjCS', 'admin', false) ON CONFLICT DO NOTHING;


--
-- Data for Name: admin_profile; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.admin_profile VALUES (1, 'สมศักดิ์ รักฟาร์ม', '0812345678', '1', NULL, 'active', '2026-07-13', 2) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (2, 'สมสู่ อาชา', '1234567890', '1', NULL, 'active', '2026-10-13', 6) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (3, 'สมชา อาสู่', '3216549870', '1', NULL, 'active', '2026-10-13', 7) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (4, 'สมรักษ์ คำสร้อย', '3456789012', '1', NULL, 'active', '2026-10-13', 8) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (5, 'สมหมาย คลายร้อน', '2034987543', '1', NULL, 'active', '2026-10-13', 9) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (6, 'สมปอง ครองใจ', '0239487554', '1', NULL, 'active', '2026-10-13', 10) ON CONFLICT DO NOTHING;


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.audit_logs VALUES (1, 'RESET_PASSWORD', 'admin1', 'worker1', '2026-10-13 15:01:52.956316', 'Reset worker password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (2, 'CHANGE_PASSWORD', 'worker1', 'worker1', '2026-10-13 15:03:45.998641', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (3, 'CREATE_WORKER', 'admin1', 'worker3', '2026-10-13 15:04:47.856499', 'Created new worker account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (4, 'CHANGE_PASSWORD', 'worker3', 'worker3', '2026-10-13 15:05:10.676985', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (5, 'CREATE_ADMIN', 'superadmin', 'admin2', '2026-10-13 16:53:44.730082', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (6, 'CHANGE_PASSWORD', 'admin2', 'admin2', '2026-10-13 16:54:04.178874', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (7, 'CREATE_ADMIN', 'superadmin', 'admin3', '2026-10-13 17:16:44.861928', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (8, 'CHANGE_PASSWORD', 'admin3', 'admin3', '2026-10-13 17:16:59.418995', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (9, 'CREATE_ADMIN', 'superadmin', 'admin4', '2026-10-13 17:26:30.347545', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (10, 'CHANGE_PASSWORD', 'admin4', 'admin4', '2026-10-13 17:26:53.219807', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (11, 'CREATE_ADMIN', 'superadmin', 'admin5', '2026-10-13 17:34:42.343415', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (12, 'CHANGE_PASSWORD', 'admin5', 'admin5', '2026-10-13 17:35:00.464448', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (13, 'CREATE_ADMIN', 'superadmin', 'admin6', '2026-10-13 17:43:04.816366', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (14, 'CHANGE_PASSWORD', 'admin6', 'admin6', '2026-10-13 17:43:27.322564', 'User changed own password') ON CONFLICT DO NOTHING;


--
-- Data for Name: farm; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.farm VALUES (1, 'ไร่อ้อยอุดมทรัพย์ (Udomsap Sugarcane Farm)', 'กาญจนบุรี (Kanchanaburi)', NULL, 3, 6, 4550, 4550, 'active', '2026-07-13') ON CONFLICT DO NOTHING;


--
-- Data for Name: farms; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--



--
-- Data for Name: worker; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.worker VALUES (2, 'สมหญิง ยิ่งเจริญ', 'หญิง', '0893334444', NULL, 'active', '1', '2026-08-13', 4) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (1, 'สมชาย หมายมั่น', 'ชาย', '0891112222', NULL, 'active', '1', '2026-08-13', 3) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (3, 'สมสู่ อาชา', 'ม้า', '1234567890', NULL, 'active', '1', '2026-10-13', 5) ON CONFLICT DO NOTHING;


--
-- Data for Name: work_logs; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.work_logs VALUES (1, 1, 'cutting', '2026-10-08', 10, 100, NULL, NULL, NULL, NULL, NULL, 2000.00, '2026-10-08', '2026-10-08') ON CONFLICT DO NOTHING;
INSERT INTO public.work_logs VALUES (2, 1, 'planting', '2026-10-09', NULL, NULL, 15, 20, NULL, NULL, NULL, 750.00, '2026-10-09', '2026-10-09') ON CONFLICT DO NOTHING;
INSERT INTO public.work_logs VALUES (3, 2, 'watering', '2026-10-11', NULL, NULL, NULL, NULL, 3, 350.00, NULL, 1050.00, '2026-10-11', '2026-10-13') ON CONFLICT DO NOTHING;
INSERT INTO public.work_logs VALUES (4, 2, 'spraying', '2026-10-12', NULL, NULL, NULL, NULL, NULL, NULL, 5, 750.00, '2026-10-12', '2026-10-12') ON CONFLICT DO NOTHING;


--
-- Data for Name: workers; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--



--
-- Name: admin_profile_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.admin_profile_id_seq', 6, true);


--
-- Name: audit_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.audit_logs_id_seq', 14, true);


--
-- Name: farm_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.farm_id_seq', 1, true);


--
-- Name: farms_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.farms_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.users_id_seq', 10, true);


--
-- Name: work_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.work_logs_id_seq', 4, true);


--
-- Name: worker_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.worker_id_seq', 3, true);


--
-- Name: workers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.workers_id_seq', 1, false);


--
-- PostgreSQL database dump complete
--


