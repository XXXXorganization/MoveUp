// src/modules/user/repository.ts
import { UserModel } from './model';
import { User, CreateUserRequest, UpdateUserRequest } from './types';
import knex from 'knex';

export class UserRepository {
  private userModel: UserModel;

  constructor(db: any) {
    this.userModel = new UserModel(db);
  }

  async findById(id: string): Promise<User | undefined> {
    return this.userModel.findById(id);
  }

  async findByPhone(phone: string): Promise<User | undefined> {
    return this.userModel.findByPhone(phone);
  }

  async create(userData: CreateUserRequest): Promise<User> {
    const defaultUserData = {
      ...userData,
      role: 'user' as const,
    };
    return this.userModel.create(defaultUserData);
  }

  async update(id: string, userData: UpdateUserRequest): Promise<User | undefined> {
    return this.userModel.update(id, userData);
  }

  async getUserStats(userId: string) {
    return this.userModel.getUserStats(userId);
  }
}