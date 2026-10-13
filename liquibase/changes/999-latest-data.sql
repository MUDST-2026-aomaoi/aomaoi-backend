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
INSERT INTO public.users VALUES (1, 'superadmin', '$2a$10$DB9kOug4gOEPKWOisyZKS.QLEkv1eJ/u.tKgN/kfnfGEA7nqWiYeC', 'superadmin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (2, 'admin1', '$2a$10$Pc1UujwH4CgKp06MWWyAGuIqgB4Gj.ljinQICskaOSodFoLViYtxO', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (3, 'worker1', '$2a$10$tIJOz2EApBXSE484cAEC5e.93.zKx/o50XFXkWIpR4qvK7JH2.Yxm', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (4, 'worker2', '$2a$10$MNrRA/qCKgPd2bLIdiE8PupMKoFfYfo.9Rjw.24Eywi5d7IroIfO.', 'worker', false) ON CONFLICT DO NOTHING;


--
-- Data for Name: admin_profile; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.admin_profile VALUES (1, 'สมศักดิ์ รักฟาร์ม', '0812345678', '1', NULL, 'active', '2026-07-13', 2) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (2, 'สมสู่ อาชา', '1234567890', '1', NULL, 'active', '2026-10-13', 6) ON CONFLICT DO NOTHING;


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.audit_logs VALUES (1, 'RESET_PASSWORD', 'admin1', 'worker1', '2026-10-13 15:01:52.956316', 'Reset worker password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (2, 'CHANGE_PASSWORD', 'worker1', 'worker1', '2026-10-13 15:03:45.998641', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (3, 'CREATE_WORKER', 'admin1', 'worker3', '2026-10-13 15:04:47.856499', 'Created new worker account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (4, 'CHANGE_PASSWORD', 'worker3', 'worker3', '2026-10-13 15:05:10.676985', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (5, 'CREATE_ADMIN', 'superadmin', 'admin2', '2026-10-13 16:53:44.730082', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (6, 'CHANGE_PASSWORD', 'admin2', 'admin2', '2026-10-13 16:54:04.178874', 'User changed own password') ON CONFLICT DO NOTHING;


--
-- Data for Name: farm; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.farm VALUES (1, 'ไร่อ้อยอุดมทรัพย์ (Udomsap Sugarcane Farm)', 'กาญจนบุรี (Kanchanaburi)', NULL, 3, 2, 4550, 4550, 'active', '2026-07-13') ON CONFLICT DO NOTHING;


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

SELECT pg_catalog.setval('public.admin_profile_id_seq', 2, true);


--
-- Name: audit_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.audit_logs_id_seq', 6, true);


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

SELECT pg_catalog.setval('public.users_id_seq', 6, true);


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


