"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createSportRoutes = void 0;
const express_1 = require("express");
const auth_1 = require("../middleware/auth");
const createSportRoutes = (sportController) => {
    const router = (0, express_1.Router)();
    // Routes
    router.post('/sport/start', auth_1.authenticate, sportController.startSportRecord.bind(sportController));
    router.put('/sport/:recordId/update', auth_1.authenticate, sportController.updateSportRecord.bind(sportController));
    router.put('/sport/:recordId/stop', auth_1.authenticate, sportController.stopSportRecord.bind(sportController));
    router.get('/sport/:recordId', auth_1.authenticate, sportController.getSportRecord.bind(sportController));
    router.get('/sport', auth_1.authenticate, sportController.getSportRecords.bind(sportController));
    router.get('/sport/:recordId/gps', auth_1.authenticate, sportController.getGpsPoints.bind(sportController));
    router.get('/sport/:recordId/heart-rate', auth_1.authenticate, sportController.getHeartRates.bind(sportController));
    return router;
};
exports.createSportRoutes = createSportRoutes;
